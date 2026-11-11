import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.urbanproblems.domain.enums.*;
import com.urbanproblems.domain.entity.*;
import com.urbanproblems.security.*;
import com.urbanproblems.repository.*;
import com.urbanproblems.service.RecurrenceService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

public class HarnessMain {
  static int pass=0, fail=0;
  static void check(String name, boolean ok){ if(ok){pass++;System.out.println("PASS "+name);} else {fail++;System.out.println("FAIL "+name);} }
  static boolean throwsAccessDenied(Runnable r){ try{ r.run(); return false; } catch(org.springframework.security.access.AccessDeniedException e){ return true; } }

  public static void main(String[] a) throws Exception {
    // ---- 1. Status transition table (real ComplaintStatus.canTransitionTo)
    check("SUBMITTED->ASSIGNED allowed", ComplaintStatus.SUBMITTED.canTransitionTo(ComplaintStatus.ASSIGNED));
    check("SUBMITTED->IN_PROGRESS rejected", !ComplaintStatus.SUBMITTED.canTransitionTo(ComplaintStatus.IN_PROGRESS));
    check("ASSIGNED->IN_PROGRESS allowed", ComplaintStatus.ASSIGNED.canTransitionTo(ComplaintStatus.IN_PROGRESS));
    check("IN_PROGRESS->PENDING_VERIFICATION allowed", ComplaintStatus.IN_PROGRESS.canTransitionTo(ComplaintStatus.PENDING_VERIFICATION));
    check("IN_PROGRESS->RESOLVED rejected (must be reviewed)", !ComplaintStatus.IN_PROGRESS.canTransitionTo(ComplaintStatus.RESOLVED));
    check("PENDING_VERIFICATION->APPROVED allowed", ComplaintStatus.PENDING_VERIFICATION.canTransitionTo(ComplaintStatus.APPROVED));
    check("PENDING_VERIFICATION->IN_PROGRESS (rework) allowed", ComplaintStatus.PENDING_VERIFICATION.canTransitionTo(ComplaintStatus.IN_PROGRESS));
    check("APPROVED->RESOLVED allowed", ComplaintStatus.APPROVED.canTransitionTo(ComplaintStatus.RESOLVED));
    check("RESOLVED is terminal", EnumSet.allOf(ComplaintStatus.class).stream().noneMatch(ComplaintStatus.RESOLVED::canTransitionTo));
    check("CANCELLED is terminal", EnumSet.allOf(ComplaintStatus.class).stream().noneMatch(ComplaintStatus.CANCELLED::canTransitionTo));
    check("no self-transition for SUBMITTED", !ComplaintStatus.SUBMITTED.canTransitionTo(ComplaintStatus.SUBMITTED));

    // ---- 2. Department routing (real ComplaintCategory)
    check("ROADS routes to Roads and Infrastructure", ComplaintCategory.ROADS_AND_POTHOLES.getDepartmentName().equals("Roads and Infrastructure"));
    check("WATER_SUPPLY routes to Water and Sanitation", ComplaintCategory.WATER_SUPPLY.getDepartmentName().equals("Water and Sanitation"));
    check("DRAINAGE routes to Water and Sanitation", ComplaintCategory.DRAINAGE_AND_SEWAGE.getDepartmentName().equals("Water and Sanitation"));
    check("ELECTRICITY routes to Electricity", ComplaintCategory.ELECTRICITY_AND_STREETLIGHTS.getDepartmentName().equals("Electricity"));
    check("WASTE routes to Waste Management", ComplaintCategory.WASTE_MANAGEMENT.getDepartmentName().equals("Waste Management"));
    Set<String> depts=new HashSet<>(); for (ComplaintCategory c: ComplaintCategory.values()) depts.add(c.getDepartmentName());
    check("five categories map to four distinct departments", depts.size()==4);

    // ---- 3. Authorization matrix (real AccessRules)
    Department water=new Department(1L), elec=new Department(2L);
    Citizen c1=new Citizen(10L), c2=new Citizen(11L);
    Complaint cA=new Complaint(100L, ComplaintCategory.WATER_SUPPLY, "Main St", 22.7, 75.8, Instant.now(), ComplaintStatus.SUBMITTED, water, c1);
    final AtomicAssign assign=new AtomicAssign();
    UserPrincipal ownerCitizen=new UserPrincipal(10L,"a","", "CITIZEN","CITIZEN",null,true);
    UserPrincipal otherCitizen=new UserPrincipal(11L,"b","", "CITIZEN","CITIZEN",null,true);
    UserPrincipal waterAdmin=new UserPrincipal(5L,"wa","", "DEPARTMENT_ADMIN",null,1L,true);
    UserPrincipal elecAdmin=new UserPrincipal(6L,"ea","", "DEPARTMENT_ADMIN",null,2L,true);
    UserPrincipal sysAdmin=new UserPrincipal(7L,"sa","", "SYSTEM_ADMIN",null,null,true);
    UserPrincipal assignedWorker=new UserPrincipal(20L,"w","", "WORKER","WORKER",1L,true);
    UserPrincipal otherWorker=new UserPrincipal(21L,"w2","", "WORKER","WORKER",1L,true);
    assign.assigned.add(cA.getId()+":20");
    AssignmentRepository repo=(cid,wid)->assign.assigned.contains(cid+":"+wid);
    check("owner citizen can view", !throwsAccessDenied(()->AccessRules.requireComplaintVisible(ownerCitizen,cA,repo)));
    check("other citizen cannot view", throwsAccessDenied(()->AccessRules.requireComplaintVisible(otherCitizen,cA,repo)));
    check("assigned worker can view", !throwsAccessDenied(()->AccessRules.requireComplaintVisible(assignedWorker,cA,repo)));
    check("unassigned worker cannot view", throwsAccessDenied(()->AccessRules.requireComplaintVisible(otherWorker,cA,repo)));
    check("same-department admin can view", !throwsAccessDenied(()->AccessRules.requireComplaintVisible(waterAdmin,cA,repo)));
    check("other-department admin cannot view", throwsAccessDenied(()->AccessRules.requireComplaintVisible(elecAdmin,cA,repo)));
    check("system admin can view", !throwsAccessDenied(()->AccessRules.requireComplaintVisible(sysAdmin,cA,repo)));
    check("ID collision: worker id 10 must not see citizen 10's complaint", throwsAccessDenied(()->AccessRules.requireComplaintVisible(
        new UserPrincipal(10L,"x","", "WORKER","WORKER",1L,true), cA, repo)));
    check("other-department admin cannot assign (requireDepartmentAdmin)", throwsAccessDenied(()->AccessRules.requireDepartmentAdmin(elecAdmin, water)));
    check("same-department admin can assign", !throwsAccessDenied(()->AccessRules.requireDepartmentAdmin(waterAdmin, water)));
    check("system admin is NOT allowed to assign (strict)", throwsAccessDenied(()->AccessRules.requireDepartmentAdmin(sysAdmin, water)));
    check("citizen cannot list a department", throwsAccessDenied(()->AccessRules.requireDepartmentScope(ownerCitizen, water)));
    check("other-department admin cannot list water", throwsAccessDenied(()->AccessRules.requireDepartmentScope(elecAdmin, water)));
    check("department admin with null department id denied", throwsAccessDenied(()->AccessRules.requireDepartmentScope(
        new UserPrincipal(9L,"n","", "DEPARTMENT_ADMIN",null,null,true), water)));

    // ---- 4. Recurrence matching (real RecurrenceService.isNearby / haversineKm)
    Instant now=Instant.now();
    Complaint base=new Complaint(1L, ComplaintCategory.WATER_SUPPLY,"Sector 4 Main Road",22.7196,75.8577,now,ComplaintStatus.SUBMITTED,water,c1);
    Complaint near=new Complaint(2L, ComplaintCategory.WATER_SUPPLY,"Different wording",22.7200,75.8580,now,ComplaintStatus.RESOLVED,water,c1); // ~0.05 km
    Complaint far=new Complaint(3L, ComplaintCategory.WATER_SUPPLY,"Sector 4 Main Road",22.7400,75.8577,now,ComplaintStatus.RESOLVED,water,c1); // ~2.2 km
    Complaint otherCat=new Complaint(4L, ComplaintCategory.ROADS_AND_POTHOLES,"Sector 4 Main Road",22.7196,75.8577,now,ComplaintStatus.RESOLVED,water,c1);
    Complaint noCoordsSameText=new Complaint(5L, ComplaintCategory.WATER_SUPPLY,"  sector 4   MAIN road ",null,null,now,ComplaintStatus.RESOLVED,water,c1);
    Complaint noCoordsOther=new Complaint(6L, ComplaintCategory.WATER_SUPPLY,"Elsewhere",null,null,now,ComplaintStatus.RESOLVED,water,c1);
    check("nearby (~50 m, different wording) matched by coordinates", isNearby(base,near));
    check("distant (~2.2 km, same wording) NOT matched", !isNearby(base,far));
    check("boundary: exactly 0.5 km matched", isNearby(new Complaint(7L,ComplaintCategory.WATER_SUPPLY,"x",0.0,0.0,now,ComplaintStatus.RESOLVED,water,c1),
        new Complaint(8L,ComplaintCategory.WATER_SUPPLY,"y",0.0,0.5/111.19492664455873,now,ComplaintStatus.RESOLVED,water,c1)));
    check("boundary: 0.6 km NOT matched", !isNearby(new Complaint(7L,ComplaintCategory.WATER_SUPPLY,"x",0.0,0.0,now,ComplaintStatus.RESOLVED,water,c1),
        new Complaint(8L,ComplaintCategory.WATER_SUPPLY,"y",0.0,0.6/111.19492664455873,now,ComplaintStatus.RESOLVED,water,c1)));
    check("both lack coordinates: same normalized text matches", isNearby(noCoordsSameText, new Complaint(9L,ComplaintCategory.WATER_SUPPLY,"Sector 4 Main Road",null,null,now,ComplaintStatus.RESOLVED,water,c1)));
    check("both lack coordinates: different text does not match", !isNearby(noCoordsSameText, noCoordsOther));
    check("one has coordinates, other does not: text fallback (different text -> no match)", !isNearby(base, noCoordsOther));
    check("haversine of identical points is zero", hav(22.7,75.8,22.7,75.8)==0.0);
    check("haversine is symmetric", Math.abs(hav(22.7,75.8,22.8,75.9)-hav(22.8,75.9,22.7,75.8))<1e-12);
    double one=hav(0,0,0,1);
    check("haversine 1 degree of longitude at equator ~111.19 km", Math.abs(one-111.195)<0.01);
    check("results reproducible across repeated calls", hav(22.7196,75.8577,22.7200,75.8580)==hav(22.7196,75.8577,22.7200,75.8580));

    System.out.println("\nRESULT: "+pass+" passed, "+fail+" failed");
    if(fail>0) System.exit(1);
  }
  static double hav(double a,double b,double c,double d){ try{ var m=RecurrenceService.class.getDeclaredMethod("haversineKm",double.class,double.class,double.class,double.class); m.setAccessible(true); return (double)m.invoke(null,a,b,c,d);}catch(Exception e){throw new RuntimeException(e);} }
  static class AtomicAssign { Set<String> assigned=new HashSet<>(); }
  static boolean isNearby(Complaint a, Complaint b) {
    try { var m=RecurrenceService.class.getDeclaredMethod("isNearby", Complaint.class, Complaint.class); m.setAccessible(true); return (boolean)m.invoke(null,a,b);} catch(Exception e){ throw new RuntimeException(e);} }
}

package com.urbanproblems.domain.entity;
import com.urbanproblems.domain.enums.*; import java.time.Instant;
public class Complaint {
  private Long id; private ComplaintCategory category; private String location; private Double latitude, longitude;
  private Instant createdAt; private ComplaintStatus status; private Department department; private Citizen citizen; private String publicId="p";
  public Complaint(Long id, ComplaintCategory c, String loc, Double lat, Double lon, Instant created, ComplaintStatus st, Department d, Citizen cz){
    this.id=id;this.category=c;this.location=loc;this.latitude=lat;this.longitude=lon;this.createdAt=created;this.status=st;this.department=d;this.citizen=cz;}
  public Long getId(){return id;} public ComplaintCategory getCategory(){return category;} public String getLocation(){return location;}
  public Double getLatitude(){return latitude;} public Double getLongitude(){return longitude;} public Instant getCreatedAt(){return createdAt;}
  public ComplaintStatus getStatus(){return status;} public Department getDepartment(){return department;} public Citizen getCitizen(){return citizen;}
  public String getPublicId(){return publicId;}
}

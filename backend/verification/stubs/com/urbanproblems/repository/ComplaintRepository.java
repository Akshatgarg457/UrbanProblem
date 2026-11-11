package com.urbanproblems.repository;
import com.urbanproblems.domain.entity.Complaint; import com.urbanproblems.domain.enums.*; import java.time.Instant; import java.util.List;
public interface ComplaintRepository { List<Complaint> findByCategoryAndStatusAndCreatedAtGreaterThanEqual(ComplaintCategory c, ComplaintStatus s, Instant since);
  long countByCategory(ComplaintCategory c); long countByCategoryAndDepartmentId(ComplaintCategory c, Long d); }

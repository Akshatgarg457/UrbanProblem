package com.urbanproblems.repository;
public interface AssignmentRepository { boolean existsByComplaintIdAndAssignedWorkerId(Long complaintId, Long workerId); }

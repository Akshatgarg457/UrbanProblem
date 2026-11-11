package com.urbanproblems.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByRecipientIdAndRecipientRoleOrderByCreatedAtDesc(
			Long recipientId, String recipientRole, Pageable pageable);

	long countByRecipientIdAndRecipientRoleAndReadFalse(Long recipientId, String recipientRole);

	@Modifying
	@Query("UPDATE Notification n SET n.read = true "
			+ "WHERE n.id = :id AND n.recipientId = :recipientId AND n.recipientRole = :recipientRole")
	int markAsRead(@Param("id") Long id,
			@Param("recipientId") Long recipientId,
			@Param("recipientRole") String recipientRole);

	@Modifying
	@Query("UPDATE Notification n SET n.read = true "
			+ "WHERE n.recipientId = :recipientId AND n.recipientRole = :recipientRole AND n.read = false")
	int markAllAsRead(@Param("recipientId") Long recipientId, @Param("recipientRole") String recipientRole);
}

package com.smartdental.repository;

import com.smartdental.entity.AuditLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query(
            value = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.actor ORDER BY a.createdAt DESC",
            countQuery = "SELECT COUNT(a) FROM AuditLog a")
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

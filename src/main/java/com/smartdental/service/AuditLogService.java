package com.smartdental.service;

import com.smartdental.entity.AuditLog;
import com.smartdental.entity.User;
import com.smartdental.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(User actor, String action, String entityType, String entityId, String details) {
        auditLogRepository.save(AuditLog.of(actor, action, entityType, entityId, details));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}

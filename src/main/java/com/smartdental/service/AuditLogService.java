package com.smartdental.service;

import com.smartdental.entity.AuditLog;
import com.smartdental.entity.User;
import com.smartdental.repository.AuditLogRepository;
import com.smartdental.repository.UserRepository;
import com.smartdental.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(User actor, String action, String entityType, String entityId, String details) {
        auditLogRepository.save(AuditLog.of(actor, action, entityType, entityId, details));
    }

    /** Resolves the current authenticated principal (if any) and records it as the actor. */
    @Transactional
    public void recordCurrentActor(String action, String entityType, String entityId, String details) {
        User actor = currentUser().orElse(null);
        record(actor, action, entityType, entityId, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    private java.util.Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return java.util.Optional.empty();
        }
        return userRepository.findById(principal.getId());
    }
}

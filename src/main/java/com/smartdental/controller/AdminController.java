package com.smartdental.controller;

import com.smartdental.dto.admin.CreateStaffUserRequest;
import com.smartdental.dto.admin.UpdateUserEnabledRequest;
import com.smartdental.dto.admin.UpdateUserRolesRequest;
import com.smartdental.dto.audit.AuditLogResponse;
import com.smartdental.dto.auth.UserSummaryResponse;
import com.smartdental.service.AdminUserService;
import com.smartdental.service.AuditLogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;

    @GetMapping("/users")
    public List<UserSummaryResponse> listUsers() {
        return adminUserService.listUsers();
    }

    @PostMapping("/users")
    public ResponseEntity<UserSummaryResponse> createStaffUser(@Valid @RequestBody CreateStaffUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createStaffUser(request));
    }

    @PatchMapping("/users/{id}/roles")
    public UserSummaryResponse updateRoles(@PathVariable UUID id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return adminUserService.updateRoles(id, request.roles());
    }

    @PatchMapping("/users/{id}/enabled")
    public UserSummaryResponse updateEnabled(
            @PathVariable UUID id, @RequestBody UpdateUserEnabledRequest request) {
        return adminUserService.updateEnabled(id, request.enabled());
    }

    @GetMapping("/audit-logs")
    public Page<AuditLogResponse> auditLogs(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return auditLogService.findAll(PageRequest.of(page, size)).map(AuditLogResponse::from);
    }
}

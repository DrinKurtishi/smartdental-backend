package com.smartdental.service;

import com.smartdental.dto.admin.CreateStaffUserRequest;
import com.smartdental.dto.auth.UserSummaryResponse;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.AuthProvider;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.exception.BadRequestException;
import com.smartdental.exception.ConflictException;
import com.smartdental.exception.ResourceNotFoundException;
import com.smartdental.repository.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listUsers() {
        return userRepository.findAll().stream().map(UserSummaryResponse::from).toList();
    }

    @Transactional
    public UserSummaryResponse createStaffUser(CreateStaffUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(true);
        user.setRoles(parseRoles(request.roles()));

        User saved = userRepository.save(user);
        auditLogService.recordCurrentActor(
                "STAFF_ACCOUNT_CREATED", "User", saved.getId().toString(), "Created " + saved.getEmail());
        return UserSummaryResponse.from(saved);
    }

    @Transactional
    public UserSummaryResponse updateRoles(UUID userId, Set<String> roles) {
        User user = getOrThrow(userId);
        user.setRoles(parseRoles(roles));
        User saved = userRepository.save(user);
        auditLogService.recordCurrentActor(
                "USER_ROLES_UPDATED", "User", saved.getId().toString(), "Roles set to " + roles);
        return UserSummaryResponse.from(saved);
    }

    @Transactional
    public UserSummaryResponse updateEnabled(UUID userId, boolean enabled) {
        User user = getOrThrow(userId);
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        auditLogService.recordCurrentActor(
                enabled ? "USER_ENABLED" : "USER_DISABLED", "User", saved.getId().toString(), saved.getEmail());
        return UserSummaryResponse.from(saved);
    }

    private Set<RoleName> parseRoles(Set<String> roles) {
        return roles.stream()
                .map(
                        r -> {
                            try {
                                return RoleName.valueOf(r.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                throw new BadRequestException("Unknown role: " + r);
                            }
                        })
                .collect(Collectors.toSet());
    }

    private User getOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}

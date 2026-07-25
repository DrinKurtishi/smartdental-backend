package com.smartdental.controller;

import com.smartdental.dto.patient.PatientProfileResponse;
import com.smartdental.dto.patient.PatientProfileUpdateRequest;
import com.smartdental.dto.patient.PatientSummaryResponse;
import com.smartdental.security.UserPrincipal;
import com.smartdental.service.PatientService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public List<PatientSummaryResponse> listPatients() {
        return patientService.listPatients();
    }

    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public PatientProfileResponse myProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return patientService.getProfile(principal.getId());
    }

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public PatientProfileResponse updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody PatientProfileUpdateRequest request) {
        return patientService.updateProfile(principal.getId(), request);
    }

    @GetMapping("/{patientUserId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public PatientProfileResponse getProfile(@PathVariable UUID patientUserId) {
        return patientService.getProfile(patientUserId);
    }

    @PutMapping("/{patientUserId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public PatientProfileResponse updateProfile(
            @PathVariable UUID patientUserId, @Valid @RequestBody PatientProfileUpdateRequest request) {
        return patientService.updateProfile(patientUserId, request);
    }
}

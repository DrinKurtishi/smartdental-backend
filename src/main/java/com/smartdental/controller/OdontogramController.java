package com.smartdental.controller;

import com.smartdental.dto.odontogram.ToothRecordResponse;
import com.smartdental.dto.odontogram.ToothRecordUpsertRequest;
import com.smartdental.security.UserPrincipal;
import com.smartdental.service.OdontogramService;
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
public class OdontogramController {

    private final OdontogramService odontogramService;

    @GetMapping("/me/odontogram")
    @PreAuthorize("hasRole('PATIENT')")
    public List<ToothRecordResponse> myOdontogram(@AuthenticationPrincipal UserPrincipal principal) {
        return odontogramService.getOdontogram(principal.getId());
    }

    @GetMapping("/{patientUserId}/odontogram")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public List<ToothRecordResponse> getOdontogram(@PathVariable UUID patientUserId) {
        return odontogramService.getOdontogram(patientUserId);
    }

    @PutMapping("/{patientUserId}/odontogram/{toothNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public ToothRecordResponse upsertTooth(
            @PathVariable UUID patientUserId,
            @PathVariable int toothNumber,
            @Valid @RequestBody ToothRecordUpsertRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return odontogramService.upsertTooth(patientUserId, toothNumber, request, principal.getId());
    }
}

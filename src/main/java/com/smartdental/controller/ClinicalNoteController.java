package com.smartdental.controller;

import com.smartdental.dto.clinicalnote.ClinicalNoteCreateRequest;
import com.smartdental.dto.clinicalnote.ClinicalNoteResponse;
import com.smartdental.dto.clinicalnote.ClinicalNoteVisibilityUpdateRequest;
import com.smartdental.security.UserPrincipal;
import com.smartdental.service.ClinicalNoteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinical-notes")
@RequiredArgsConstructor
public class ClinicalNoteController {

    private final ClinicalNoteService clinicalNoteService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public List<ClinicalNoteResponse> myNotes(@AuthenticationPrincipal UserPrincipal principal) {
        return clinicalNoteService.findForPatient(principal.getId(), true);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public List<ClinicalNoteResponse> forPatient(@PathVariable UUID patientId) {
        return clinicalNoteService.findForPatient(patientId, false);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DENTIST', 'HYGIENIST')")
    public ResponseEntity<ClinicalNoteResponse> create(
            @Valid @RequestBody ClinicalNoteCreateRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        ClinicalNoteResponse created = clinicalNoteService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public ClinicalNoteResponse updateVisibility(
            @PathVariable UUID id, @RequestBody ClinicalNoteVisibilityUpdateRequest request) {
        return clinicalNoteService.setVisibility(id, request.patientVisible());
    }
}

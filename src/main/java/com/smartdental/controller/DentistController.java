package com.smartdental.controller;

import com.smartdental.dto.patient.PatientSummaryResponse;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only dentist directory so staff can pick whose calendar to view when scheduling. */
@RestController
@RequestMapping("/api/v1/dentists")
@RequiredArgsConstructor
public class DentistController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public List<PatientSummaryResponse> listDentists() {
        return userRepository.findByRolesContainingOrderByLastNameAsc(RoleName.ROLE_DENTIST).stream()
                .map(PatientSummaryResponse::from)
                .toList();
    }
}

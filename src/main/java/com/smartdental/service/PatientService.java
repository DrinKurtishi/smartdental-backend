package com.smartdental.service;

import com.smartdental.dto.patient.PatientProfileResponse;
import com.smartdental.dto.patient.PatientProfileUpdateRequest;
import com.smartdental.dto.patient.PatientSummaryResponse;
import com.smartdental.entity.PatientProfile;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.BloodType;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.exception.BadRequestException;
import com.smartdental.exception.ResourceNotFoundException;
import com.smartdental.repository.PatientProfileRepository;
import com.smartdental.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;

    @Transactional(readOnly = true)
    public java.util.List<PatientSummaryResponse> listPatients() {
        return userRepository.findByRolesContainingOrderByLastNameAsc(RoleName.ROLE_PATIENT).stream()
                .map(PatientSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(UUID patientUserId) {
        return PatientProfileResponse.from(getOrCreateProfile(patientUserId));
    }

    @Transactional
    public PatientProfileResponse updateProfile(UUID patientUserId, PatientProfileUpdateRequest request) {
        PatientProfile profile = getOrCreateProfile(patientUserId);

        profile.setDateOfBirth(request.dateOfBirth());
        if (request.bloodType() != null) {
            profile.setBloodType(parseBloodType(request.bloodType()));
        }
        profile.setEmergencyContactName(request.emergencyContactName());
        profile.setEmergencyContactPhone(request.emergencyContactPhone());
        profile.setInsuranceProvider(request.insuranceProvider());
        profile.setInsurancePolicyNumber(request.insurancePolicyNumber());
        profile.setInsuranceGroupNumber(request.insuranceGroupNumber());
        profile.setInsuranceCoveragePercent(request.insuranceCoveragePercent());
        if (request.medicalAlerts() != null) {
            profile.setMedicalAlerts(request.medicalAlerts());
        }
        profile.setMedicalNotes(request.medicalNotes());

        return PatientProfileResponse.from(patientProfileRepository.save(profile));
    }

    @Transactional
    public PatientProfile getOrCreateProfile(UUID patientUserId) {
        return patientProfileRepository
                .findByUserId(patientUserId)
                .orElseGet(
                        () -> {
                            User user =
                                    userRepository
                                            .findById(patientUserId)
                                            .orElseThrow(
                                                    () -> new ResourceNotFoundException("Patient not found: " + patientUserId));
                            PatientProfile profile = new PatientProfile();
                            profile.setUser(user);
                            return patientProfileRepository.save(profile);
                        });
    }

    private BloodType parseBloodType(String raw) {
        try {
            return BloodType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown blood type: " + raw);
        }
    }
}

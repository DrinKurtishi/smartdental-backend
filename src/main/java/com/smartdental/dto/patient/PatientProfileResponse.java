package com.smartdental.dto.patient;

import com.smartdental.entity.PatientProfile;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record PatientProfileResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String bloodType,
        String emergencyContactName,
        String emergencyContactPhone,
        String insuranceProvider,
        String insurancePolicyNumber,
        String insuranceGroupNumber,
        Integer insuranceCoveragePercent,
        Set<String> medicalAlerts,
        String medicalNotes) {

    public static PatientProfileResponse from(PatientProfile profile) {
        return new PatientProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getUser().getEmail(),
                profile.getUser().getPhone(),
                profile.getDateOfBirth(),
                profile.getBloodType().name(),
                profile.getEmergencyContactName(),
                profile.getEmergencyContactPhone(),
                profile.getInsuranceProvider(),
                profile.getInsurancePolicyNumber(),
                profile.getInsuranceGroupNumber(),
                profile.getInsuranceCoveragePercent(),
                profile.getMedicalAlerts(),
                profile.getMedicalNotes());
    }
}

package com.smartdental.dto.patient;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.Set;

public record PatientProfileUpdateRequest(
        LocalDate dateOfBirth,
        String bloodType,
        String emergencyContactName,
        String emergencyContactPhone,
        String insuranceProvider,
        String insurancePolicyNumber,
        String insuranceGroupNumber,
        @Min(0) @Max(100) Integer insuranceCoveragePercent,
        Set<String> medicalAlerts,
        String medicalNotes) {}

package com.smartdental.dto.clinicalnote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ClinicalNoteCreateRequest(
        @NotNull UUID patientId,
        UUID appointmentId,
        @NotBlank String shorthand,
        String aiSummary,
        String cdtCode,
        Integer toothNumber,
        boolean patientVisible) {}

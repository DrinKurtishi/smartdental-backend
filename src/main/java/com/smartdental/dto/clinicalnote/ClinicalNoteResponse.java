package com.smartdental.dto.clinicalnote;

import com.smartdental.entity.ClinicalNote;
import java.time.Instant;
import java.util.UUID;

public record ClinicalNoteResponse(
        UUID id,
        UUID appointmentId,
        UUID patientId,
        String patientName,
        UUID dentistId,
        String dentistName,
        String shorthand,
        String aiSummary,
        String cdtCode,
        Integer toothNumber,
        boolean patientVisible,
        Instant createdAt) {

    public static ClinicalNoteResponse from(ClinicalNote note) {
        return new ClinicalNoteResponse(
                note.getId(),
                note.getAppointment() != null ? note.getAppointment().getId() : null,
                note.getPatient().getId(),
                note.getPatient().getFullName(),
                note.getDentist().getId(),
                note.getDentist().getFullName(),
                note.getShorthand(),
                note.getAiSummary(),
                note.getCdtCode(),
                note.getToothNumber(),
                note.isPatientVisible(),
                note.getCreatedAt());
    }
}

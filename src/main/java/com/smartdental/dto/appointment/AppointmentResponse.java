package com.smartdental.dto.appointment;

import com.smartdental.entity.Appointment;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        String patientName,
        UUID dentistId,
        String dentistName,
        Instant startTime,
        Instant endTime,
        String status,
        String reason,
        String notes) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFullName(),
                appointment.getDentist().getId(),
                appointment.getDentist().getFullName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus().name(),
                appointment.getReason(),
                appointment.getNotes());
    }
}

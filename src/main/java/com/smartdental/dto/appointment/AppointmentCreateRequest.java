package com.smartdental.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AppointmentCreateRequest(
        UUID patientId,
        @NotNull UUID dentistId,
        @NotNull @Future Instant startTime,
        @NotNull @Future Instant endTime,
        @NotBlank String reason,
        String notes) {}

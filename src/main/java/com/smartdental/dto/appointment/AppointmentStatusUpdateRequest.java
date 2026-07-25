package com.smartdental.dto.appointment;

import jakarta.validation.constraints.NotBlank;

public record AppointmentStatusUpdateRequest(@NotBlank String status) {}

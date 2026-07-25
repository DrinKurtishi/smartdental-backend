package com.smartdental.dto.invoice;

import jakarta.validation.constraints.NotBlank;

public record InvoiceStatusUpdateRequest(@NotBlank String status) {}

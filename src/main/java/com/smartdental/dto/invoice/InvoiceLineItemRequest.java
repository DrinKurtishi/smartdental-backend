package com.smartdental.dto.invoice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InvoiceLineItemRequest(
        @NotBlank String description,
        String cdtCode,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        @DecimalMin("0.00") BigDecimal insuranceCoveredAmount) {}

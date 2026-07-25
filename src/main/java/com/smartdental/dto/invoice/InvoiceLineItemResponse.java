package com.smartdental.dto.invoice;

import com.smartdental.entity.InvoiceLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineItemResponse(
        UUID id,
        String description,
        String cdtCode,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal insuranceCoveredAmount,
        BigDecimal lineTotal) {

    public static InvoiceLineItemResponse from(InvoiceLineItem item) {
        return new InvoiceLineItemResponse(
                item.getId(),
                item.getDescription(),
                item.getCdtCode(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getInsuranceCoveredAmount(),
                item.getLineTotal());
    }
}

package com.smartdental.dto.invoice;

import com.smartdental.entity.Invoice;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID patientId,
        String patientName,
        UUID appointmentId,
        String status,
        List<InvoiceLineItemResponse> lineItems,
        BigDecimal subtotal,
        BigDecimal insuranceCoveredAmount,
        BigDecimal balanceDue,
        LocalDate dueDate,
        Instant createdAt) {

    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getPatient().getId(),
                invoice.getPatient().getFullName(),
                invoice.getAppointment() != null ? invoice.getAppointment().getId() : null,
                invoice.getStatus().name(),
                invoice.getLineItems().stream().map(InvoiceLineItemResponse::from).toList(),
                invoice.getSubtotal(),
                invoice.getInsuranceCoveredAmount(),
                invoice.getBalanceDue(),
                invoice.getDueDate(),
                invoice.getCreatedAt());
    }
}

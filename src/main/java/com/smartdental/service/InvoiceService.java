package com.smartdental.service;

import com.smartdental.dto.invoice.InvoiceCreateRequest;
import com.smartdental.dto.invoice.InvoiceLineItemRequest;
import com.smartdental.dto.invoice.InvoiceResponse;
import com.smartdental.entity.Appointment;
import com.smartdental.entity.Invoice;
import com.smartdental.entity.InvoiceLineItem;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.InvoiceStatus;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.exception.BadRequestException;
import com.smartdental.exception.ResourceNotFoundException;
import com.smartdental.repository.AppointmentRepository;
import com.smartdental.repository.InvoiceRepository;
import com.smartdental.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findForPatient(UUID patientId) {
        return invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll() {
        return invoiceRepository.findAll().stream().map(InvoiceResponse::from).toList();
    }

    @Transactional
    public InvoiceResponse create(InvoiceCreateRequest request) {
        User patient =
                userRepository
                        .findById(request.patientId())
                        .filter(u -> u.getRoles().contains(RoleName.ROLE_PATIENT))
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.patientId()));

        Invoice invoice = new Invoice();
        invoice.setPatient(patient);
        invoice.setDueDate(request.dueDate());
        invoice.setStatus(InvoiceStatus.UNPAID);

        if (request.appointmentId() != null) {
            Appointment appointment =
                    appointmentRepository
                            .findById(request.appointmentId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Appointment not found: " + request.appointmentId()));
            invoice.setAppointment(appointment);
        }

        for (InvoiceLineItemRequest lineItemRequest : request.lineItems()) {
            InvoiceLineItem lineItem = new InvoiceLineItem();
            lineItem.setInvoice(invoice);
            lineItem.setDescription(lineItemRequest.description());
            lineItem.setCdtCode(lineItemRequest.cdtCode());
            lineItem.setQuantity(lineItemRequest.quantity());
            lineItem.setUnitPrice(lineItemRequest.unitPrice());
            lineItem.setInsuranceCoveredAmount(
                    lineItemRequest.insuranceCoveredAmount() != null
                            ? lineItemRequest.insuranceCoveredAmount()
                            : BigDecimal.ZERO);
            invoice.getLineItems().add(lineItem);
        }

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse updateStatus(UUID invoiceId, InvoiceStatus status) {
        Invoice invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        invoice.setStatus(status);
        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    public static InvoiceStatus parseStatus(String raw) {
        try {
            return InvoiceStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown invoice status: " + raw);
        }
    }
}

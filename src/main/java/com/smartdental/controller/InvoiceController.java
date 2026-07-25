package com.smartdental.controller;

import com.smartdental.dto.invoice.InvoiceCreateRequest;
import com.smartdental.dto.invoice.InvoiceResponse;
import com.smartdental.dto.invoice.InvoiceStatusUpdateRequest;
import com.smartdental.security.UserPrincipal;
import com.smartdental.service.InvoiceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public List<InvoiceResponse> myInvoices(@AuthenticationPrincipal UserPrincipal principal) {
        return invoiceService.findForPatient(principal.getId());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public List<InvoiceResponse> allInvoices() {
        return invoiceService.findAll();
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public List<InvoiceResponse> forPatient(@PathVariable UUID patientId) {
        return invoiceService.findForPatient(patientId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public InvoiceResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody InvoiceStatusUpdateRequest request) {
        return invoiceService.updateStatus(id, InvoiceService.parseStatus(request.status()));
    }
}

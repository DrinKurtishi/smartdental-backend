package com.smartdental.entity;

import com.smartdental.entity.enums.InvoiceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "invoices",
        indexes = {@Index(name = "idx_invoices_patient", columnList = "patient_id")})
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @Column(name = "due_date")
    private LocalDate dueDate;

    public BigDecimal getSubtotal() {
        return lineItems.stream().map(InvoiceLineItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getInsuranceCoveredAmount() {
        return lineItems.stream()
                .map(InvoiceLineItem::getInsuranceCoveredAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getBalanceDue() {
        return getSubtotal().subtract(getInsuranceCoveredAmount());
    }
}

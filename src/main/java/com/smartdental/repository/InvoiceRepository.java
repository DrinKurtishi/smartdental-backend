package com.smartdental.repository;

import com.smartdental.entity.Invoice;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}

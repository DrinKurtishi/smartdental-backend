package com.smartdental.repository;

import com.smartdental.entity.InvoiceLineItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {}

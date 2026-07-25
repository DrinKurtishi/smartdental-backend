package com.smartdental.repository;

import com.smartdental.entity.ToothRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToothRecordRepository extends JpaRepository<ToothRecord, UUID> {

    List<ToothRecord> findByPatientProfileIdOrderByToothNumberAsc(UUID patientProfileId);

    Optional<ToothRecord> findByPatientProfileIdAndToothNumber(UUID patientProfileId, int toothNumber);
}

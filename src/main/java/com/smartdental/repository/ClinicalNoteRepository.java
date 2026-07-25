package com.smartdental.repository;

import com.smartdental.entity.ClinicalNote;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID> {

    List<ClinicalNote> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<ClinicalNote> findByPatientIdAndPatientVisibleTrueOrderByCreatedAtDesc(UUID patientId);
}

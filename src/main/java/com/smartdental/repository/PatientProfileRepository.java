package com.smartdental.repository;

import com.smartdental.entity.PatientProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {

    Optional<PatientProfile> findByUserId(UUID userId);
}

package com.smartdental.repository;

import com.smartdental.entity.Appointment;
import com.smartdental.entity.enums.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientIdOrderByStartTimeDesc(UUID patientId);

    List<Appointment> findByDentistIdAndStartTimeBetweenOrderByStartTimeAsc(
            UUID dentistId, Instant rangeStart, Instant rangeEnd);

    @Query(
            """
            SELECT a FROM Appointment a
            WHERE a.dentist.id = :dentistId
              AND a.status <> com.smartdental.entity.enums.AppointmentStatus.CANCELLED
              AND (:excludeId IS NULL OR a.id <> :excludeId)
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    List<Appointment> findOverlapping(
            @Param("dentistId") UUID dentistId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("excludeId") UUID excludeId);

    List<Appointment> findByStatusOrderByStartTimeAsc(AppointmentStatus status);
}

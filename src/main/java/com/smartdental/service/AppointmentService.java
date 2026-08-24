package com.smartdental.service;

import com.smartdental.dto.appointment.AppointmentCreateRequest;
import com.smartdental.dto.appointment.AppointmentResponse;
import com.smartdental.entity.Appointment;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.AppointmentStatus;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.exception.BadRequestException;
import com.smartdental.exception.ConflictException;
import com.smartdental.exception.ResourceNotFoundException;
import com.smartdental.repository.AppointmentRepository;
import com.smartdental.repository.UserRepository;
import com.smartdental.service.email.SesMailService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final SesMailService sesMailService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findForPatient(UUID patientId) {
        return appointmentRepository.findByPatientIdOrderByStartTimeDesc(patientId).stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findForDentistBetween(UUID dentistId, Instant start, Instant end) {
        return appointmentRepository
                .findByDentistIdAndStartTimeBetweenOrderByStartTimeAsc(dentistId, start, end)
                .stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(UUID id) {
        return AppointmentResponse.from(getAppointmentOrThrow(id));
    }

    @Transactional(readOnly = true)
    public String generateIcs(UUID id) {
        return IcsGenerator.generate(getAppointmentOrThrow(id));
    }

    @Transactional
    public Appointment create(AppointmentCreateRequest request, UUID requesterId, boolean requesterIsStaff) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("Appointment end time must be after the start time");
        }

        UUID patientId = requesterIsStaff ? request.patientId() : requesterId;
        if (patientId == null) {
            throw new BadRequestException("patientId is required when a staff member books on a patient's behalf");
        }

        User patient = loadUserWithRole(patientId, RoleName.ROLE_PATIENT, "Patient");
        User dentist = loadUserWithRole(request.dentistId(), RoleName.ROLE_DENTIST, "Dentist");

        assertNoCollision(dentist.getId(), request.startTime(), request.endTime(), null);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDentist(dentist);
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        appointment.setReason(request.reason());
        appointment.setNotes(request.notes());
        appointment.setStatus(AppointmentStatus.PENDING);

        Appointment saved = appointmentRepository.save(appointment);
        auditLogService.recordCurrentActor(
                "APPOINTMENT_CREATED",
                "Appointment",
                saved.getId().toString(),
                patient.getFullName() + " with Dr. " + dentist.getFullName());
        sesMailService.sendAppointmentCreated(saved);
        return saved;
    }

    @Transactional
    public Appointment updateStatus(UUID appointmentId, AppointmentStatus newStatus) {
        Appointment appointment = getAppointmentOrThrow(appointmentId);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("A cancelled appointment cannot change status");
        }
        if (newStatus == AppointmentStatus.CONFIRMED) {
            assertNoCollision(
                    appointment.getDentist().getId(),
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    appointment.getId());
        }

        appointment.setStatus(newStatus);
        Appointment saved = appointmentRepository.save(appointment);

        auditLogService.recordCurrentActor(
                "APPOINTMENT_STATUS_CHANGED", "Appointment", saved.getId().toString(), "Status set to " + newStatus);

        if (newStatus == AppointmentStatus.CANCELLED) {
            sesMailService.sendAppointmentCancelled(saved);
        } else {
            sesMailService.sendAppointmentStatusChanged(saved);
        }
        return saved;
    }

    @Transactional
    public Appointment cancel(UUID appointmentId) {
        return updateStatus(appointmentId, AppointmentStatus.CANCELLED);
    }

    private void assertNoCollision(UUID dentistId, Instant start, Instant end, UUID excludeAppointmentId) {
        List<Appointment> overlapping =
                appointmentRepository.findOverlapping(dentistId, start, end, excludeAppointmentId);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("The dentist already has an appointment in this time slot");
        }
    }

    private User loadUserWithRole(UUID userId, RoleName role, String label) {
        User user =
                userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException(label + " not found"));
        if (!user.getRoles().contains(role)) {
            throw new BadRequestException(label + " account does not have the " + role + " role");
        }
        return user;
    }

    Appointment getAppointmentOrThrow(UUID id) {
        return appointmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }
}

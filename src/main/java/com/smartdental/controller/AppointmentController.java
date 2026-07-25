package com.smartdental.controller;

import com.smartdental.dto.appointment.AppointmentCreateRequest;
import com.smartdental.dto.appointment.AppointmentResponse;
import com.smartdental.dto.appointment.AppointmentStatusUpdateRequest;
import com.smartdental.entity.Appointment;
import com.smartdental.entity.enums.AppointmentStatus;
import com.smartdental.exception.BadRequestException;
import com.smartdental.exception.ResourceNotFoundException;
import com.smartdental.security.UserPrincipal;
import com.smartdental.service.AppointmentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public List<AppointmentResponse> myAppointments(@AuthenticationPrincipal UserPrincipal principal) {
        return appointmentService.findForPatient(principal.getId());
    }

    @GetMapping("/dentist/{dentistId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public List<AppointmentResponse> dentistSchedule(
            @PathVariable UUID dentistId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        if (!to.isAfter(from)) {
            throw new BadRequestException("'to' must be after 'from'");
        }
        return appointmentService.findForDentistBetween(dentistId, from, to);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AppointmentResponse getAppointment(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        AppointmentResponse appointment = appointmentService.findById(id);
        assertPatientOwnsOrIsStaff(appointment, principal);
        return appointment;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST', 'PATIENT')")
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody AppointmentCreateRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStaff = isStaff(principal);
        Appointment created = appointmentService.create(request, principal.getId(), isStaff);
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentResponse.from(created));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public AppointmentResponse updateStatus(
            @PathVariable UUID id, @Valid @RequestBody AppointmentStatusUpdateRequest request) {
        AppointmentStatus status = parseStatus(request.status());
        return AppointmentResponse.from(appointmentService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AppointmentResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        AppointmentResponse existing = appointmentService.findById(id);
        assertPatientOwnsOrIsStaff(existing, principal);
        return AppointmentResponse.from(appointmentService.cancel(id));
    }

    @GetMapping(value = "/{id}/ics", produces = "text/calendar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> exportIcs(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        AppointmentResponse existing = appointmentService.findById(id);
        assertPatientOwnsOrIsStaff(existing, principal);

        String ics = appointmentService.generateIcs(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"appointment-" + id + ".ics\"")
                .body(ics);
    }

    private void assertPatientOwnsOrIsStaff(AppointmentResponse appointment, UserPrincipal principal) {
        if (isStaff(principal)) {
            return;
        }
        if (!appointment.patientId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Appointment not found: " + appointment.id());
        }
    }

    private boolean isStaff(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .anyMatch(
                        a ->
                                a.getAuthority().equals("ROLE_ADMIN")
                                        || a.getAuthority().equals("ROLE_DENTIST")
                                        || a.getAuthority().equals("ROLE_HYGIENIST"));
    }

    private AppointmentStatus parseStatus(String raw) {
        try {
            return AppointmentStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown appointment status: " + raw);
        }
    }
}

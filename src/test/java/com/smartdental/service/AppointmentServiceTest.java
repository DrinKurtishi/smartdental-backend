package com.smartdental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.smartdental.dto.appointment.AppointmentCreateRequest;
import com.smartdental.entity.Appointment;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.exception.ConflictException;
import com.smartdental.repository.AppointmentRepository;
import com.smartdental.repository.UserRepository;
import com.smartdental.service.email.SesMailService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private SesMailService sesMailService;
    @Mock private AuditLogService auditLogService;

    private AppointmentService appointmentService;

    private User patient;
    private User dentist;
    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        appointmentService =
                new AppointmentService(appointmentRepository, userRepository, sesMailService, auditLogService);

        patient = buildUser(RoleName.ROLE_PATIENT);
        dentist = buildUser(RoleName.ROLE_DENTIST);
        start = Instant.now().plus(1, ChronoUnit.DAYS);
        end = start.plus(30, ChronoUnit.MINUTES);
    }

    @Test
    void bookingIsRejectedWhenDentistAlreadyHasAnOverlappingAppointment() {
        AppointmentCreateRequest request = new AppointmentCreateRequest(patient.getId(), dentist.getId(), start, end, "Checkup", null);

        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(userRepository.findById(dentist.getId())).thenReturn(Optional.of(dentist));
        when(appointmentRepository.findOverlapping(eq(dentist.getId()), eq(start), eq(end), isNull()))
                .thenReturn(List.of(new Appointment()));

        assertThatThrownBy(() -> appointmentService.create(request, patient.getId(), false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has an appointment");
    }

    @Test
    void bookingSucceedsAndNotifiesWhenNoCollisionExists() {
        AppointmentCreateRequest request = new AppointmentCreateRequest(patient.getId(), dentist.getId(), start, end, "Checkup", null);

        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(userRepository.findById(dentist.getId())).thenReturn(Optional.of(dentist));
        when(appointmentRepository.findOverlapping(eq(dentist.getId()), eq(start), eq(end), isNull()))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(this::assignIdAndReturn);

        Appointment created = appointmentService.create(request, patient.getId(), false);

        assertThat(created.getPatient()).isEqualTo(patient);
        assertThat(created.getDentist()).isEqualTo(dentist);
        org.mockito.Mockito.verify(sesMailService).sendAppointmentCreated(created);
    }

    @Test
    void nonStaffCallerAlwaysBooksForThemselvesEvenIfAnotherPatientIdIsSupplied() {
        UUID spoofedPatientId = UUID.randomUUID();
        AppointmentCreateRequest request =
                new AppointmentCreateRequest(spoofedPatientId, dentist.getId(), start, end, "Checkup", null);

        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(userRepository.findById(dentist.getId())).thenReturn(Optional.of(dentist));
        when(appointmentRepository.findOverlapping(eq(dentist.getId()), eq(start), eq(end), isNull()))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(this::assignIdAndReturn);

        Appointment created = appointmentService.create(request, patient.getId(), false);

        assertThat(created.getPatient().getId()).isEqualTo(patient.getId());
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).findById(spoofedPatientId);
    }

    private Appointment assignIdAndReturn(org.mockito.invocation.InvocationOnMock invocation) {
        Appointment appointment = invocation.getArgument(0);
        if (appointment.getId() == null) {
            setId(appointment, UUID.randomUUID());
        }
        return appointment;
    }

    private User buildUser(RoleName role) {
        User user = new User();
        user.setEmail(role.name().toLowerCase() + "@example.com");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setRoles(Set.of(role));
        setId(user, UUID.randomUUID());
        return user;
    }

    private static void setId(com.smartdental.entity.BaseEntity entity, UUID id) {
        try {
            var field = com.smartdental.entity.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

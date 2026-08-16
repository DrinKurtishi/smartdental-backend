package com.smartdental.service.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.smartdental.config.AwsProperties;
import com.smartdental.entity.Appointment;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.AppointmentStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class SesMailServiceTest {

    @Mock private SesClient sesClient;

    private Appointment appointment;

    @BeforeEach
    void setUp() {
        User patient = new User();
        patient.setEmail("sofia.nguyen@example.com");
        patient.setFirstName("Sofia");
        patient.setLastName("Nguyen");

        User dentist = new User();
        dentist.setEmail("dr.rivera@smartdental.example.com");
        dentist.setFirstName("Elena");
        dentist.setLastName("Rivera");

        appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDentist(dentist);
        appointment.setReason("Routine cleaning and checkup");
        appointment.setStartTime(Instant.now().plus(2, ChronoUnit.DAYS));
        appointment.setEndTime(appointment.getStartTime().plus(60, ChronoUnit.MINUTES));
        appointment.setStatus(AppointmentStatus.CONFIRMED);
    }

    @Test
    void sendsToBothPatientAndDentistWhenSesIsEnabled() {
        AwsProperties properties =
                new AwsProperties("us-east-1", new AwsProperties.Ses("no-reply@smartdental.example.com", "", "", true));
        SesMailService mailService = new SesMailService(sesClient, properties);
        org.mockito.Mockito.when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("test-message-id").build());

        mailService.sendAppointmentCreated(appointment);

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient, times(2)).sendEmail(captor.capture());

        var requests = captor.getAllValues();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).source()).isEqualTo("no-reply@smartdental.example.com");
        assertThat(requests)
                .extracting(r -> r.destination().toAddresses().get(0))
                .containsExactlyInAnyOrder("sofia.nguyen@example.com", "dr.rivera@smartdental.example.com");

        String html = requests.get(0).message().body().html().data();
        assertThat(html).contains("SmartDental");
        assertThat(html).contains("Routine cleaning and checkup");
        assertThat(html).contains("Dr. Elena Rivera");
        assertThat(html).contains("CONFIRMED");
    }

    @Test
    void skipsSendingWhenSesIsDisabled() {
        AwsProperties properties =
                new AwsProperties("us-east-1", new AwsProperties.Ses("no-reply@smartdental.example.com", "", "", false));
        SesMailService mailService = new SesMailService(sesClient, properties);

        mailService.sendAppointmentCancelled(appointment);

        verify(sesClient, never()).sendEmail(any(SendEmailRequest.class));
    }
}

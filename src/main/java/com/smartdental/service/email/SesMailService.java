package com.smartdental.service.email;

import com.smartdental.config.AwsProperties;
import com.smartdental.entity.Appointment;
import com.smartdental.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

/** Dispatches styled HTML appointment notifications to both patient and dentist via AWS SES. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SesMailService {

    private final SesClient sesClient;
    private final AwsProperties awsProperties;

    public void sendAppointmentCreated(Appointment appointment) {
        dispatch(appointment, "Your SmartDental appointment request was received");
    }

    public void sendAppointmentStatusChanged(Appointment appointment) {
        dispatch(appointment, "Your SmartDental appointment status has been updated");
    }

    public void sendAppointmentCancelled(Appointment appointment) {
        dispatch(appointment, "Your SmartDental appointment has been cancelled");
    }

    private void dispatch(Appointment appointment, String headline) {
        if (!awsProperties.ses().enabled()) {
            log.info(
                    "SES disabled - skipping notification for appointment {} ({})",
                    appointment.getId(),
                    headline);
            return;
        }

        send(appointment.getPatient(), appointment, headline);
        send(appointment.getDentist(), appointment, headline);
    }

    private void send(User recipient, Appointment appointment, String headline) {
        String html = EmailTemplates.appointmentNotification(appointment, headline, recipient.getFullName());

        SendEmailRequest request =
                SendEmailRequest.builder()
                        .source(awsProperties.ses().sender())
                        .destination(Destination.builder().toAddresses(recipient.getEmail()).build())
                        .message(
                                Message.builder()
                                        .subject(Content.builder().data(headline).charset("UTF-8").build())
                                        .body(Body.builder().html(Content.builder().data(html).charset("UTF-8").build()).build())
                                        .build())
                        .build();

        try {
            sesClient.sendEmail(request);
            log.info("Sent '{}' notification to {}", headline, recipient.getEmail());
        } catch (SesException e) {
            log.error("Failed to send SES notification to {}: {}", recipient.getEmail(), e.awsErrorDetails().errorMessage());
        }
    }
}

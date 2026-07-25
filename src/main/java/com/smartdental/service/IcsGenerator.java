package com.smartdental.service;

import com.smartdental.entity.Appointment;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Builds a minimal RFC 5545 .ics file so patients can add appointments to their calendar. */
public final class IcsGenerator {

    private static final DateTimeFormatter STAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private IcsGenerator() {}

    public static String generate(Appointment appointment) {
        String uid = appointment.getId() + "@smartdental.example.com";
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//SmartDental//Appointment//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(uid).append("\r\n");
        sb.append("DTSTAMP:").append(STAMP_FORMAT.format(appointment.getCreatedAt())).append("\r\n");
        sb.append("DTSTART:").append(STAMP_FORMAT.format(appointment.getStartTime())).append("\r\n");
        sb.append("DTEND:").append(STAMP_FORMAT.format(appointment.getEndTime())).append("\r\n");
        sb.append("SUMMARY:").append(escape("Dental appointment: " + appointment.getReason())).append("\r\n");
        sb.append("DESCRIPTION:")
                .append(escape("Appointment with Dr. " + appointment.getDentist().getFullName()))
                .append("\r\n");
        sb.append("STATUS:").append(appointment.getStatus() == com.smartdental.entity.enums.AppointmentStatus.CONFIRMED
                ? "CONFIRMED"
                : "TENTATIVE")
                .append("\r\n");
        sb.append("END:VEVENT\r\n");
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }
}

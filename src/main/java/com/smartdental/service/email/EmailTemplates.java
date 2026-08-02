package com.smartdental.service.email;

import com.smartdental.entity.Appointment;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/** Builds the styled transactional HTML used for appointment notification emails. */
public final class EmailTemplates {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withZone(ZoneId.of("UTC"));

    private EmailTemplates() {}

    public static String appointmentNotification(Appointment appointment, String headline, String recipientLabel) {
        String when = DATE_TIME_FORMAT.format(appointment.getStartTime()) + " UTC";
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:'Segoe UI',Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 1px 4px rgba(15,23,42,0.08);">
                          <tr>
                            <td style="background-color:#0f766e;padding:24px 32px;">
                              <span style="color:#ffffff;font-size:20px;font-weight:600;letter-spacing:0.3px;">SmartDental</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <h1 style="margin:0 0 8px;font-size:20px;color:#0f172a;">%s</h1>
                              <p style="margin:0 0 24px;color:#475569;font-size:14px;">Hello %s,</p>
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc;border-radius:8px;padding:16px;margin-bottom:24px;">
                                <tr><td style="padding:4px 0;color:#64748b;font-size:13px;">Reason</td><td style="padding:4px 0;color:#0f172a;font-size:13px;font-weight:600;text-align:right;">%s</td></tr>
                                <tr><td style="padding:4px 0;color:#64748b;font-size:13px;">When</td><td style="padding:4px 0;color:#0f172a;font-size:13px;font-weight:600;text-align:right;">%s</td></tr>
                                <tr><td style="padding:4px 0;color:#64748b;font-size:13px;">Dentist</td><td style="padding:4px 0;color:#0f172a;font-size:13px;font-weight:600;text-align:right;">Dr. %s</td></tr>
                                <tr><td style="padding:4px 0;color:#64748b;font-size:13px;">Status</td><td style="padding:4px 0;color:#0f172a;font-size:13px;font-weight:600;text-align:right;">%s</td></tr>
                              </table>
                              <p style="margin:0;color:#94a3b8;font-size:12px;">This is an automated message from SmartDental. Please do not reply directly to this email.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """
                .formatted(
                        headline,
                        recipientLabel,
                        escapeHtml(appointment.getReason()),
                        when,
                        escapeHtml(appointment.getDentist().getFullName()),
                        appointment.getStatus().name());
    }

    private static String escapeHtml(String value) {
        return value == null
                ? ""
                : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

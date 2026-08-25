package com.smartdental.seed;

import com.smartdental.entity.Appointment;
import com.smartdental.entity.ClinicalNote;
import com.smartdental.entity.Invoice;
import com.smartdental.entity.InvoiceLineItem;
import com.smartdental.entity.PatientProfile;
import com.smartdental.entity.ToothRecord;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.AppointmentStatus;
import com.smartdental.entity.enums.AuthProvider;
import com.smartdental.entity.enums.BloodType;
import com.smartdental.entity.enums.InvoiceStatus;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.entity.enums.ToothCondition;
import com.smartdental.entity.enums.ToothSurface;
import com.smartdental.repository.AppointmentRepository;
import com.smartdental.repository.ClinicalNoteRepository;
import com.smartdental.repository.InvoiceRepository;
import com.smartdental.repository.PatientProfileRepository;
import com.smartdental.repository.ToothRecordRepository;
import com.smartdental.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Populates a demo clinic (dentists, patients, appointments, odontogram states, notes, invoices). */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "smartdental.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final ToothRecordRepository toothRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data, skipping seed");
            return;
        }

        log.info("Seeding SmartDental demo data...");

        User admin = createUser("admin@smartdental.example.com", "Alex", "Morgan", "+1-555-0100", RoleName.ROLE_ADMIN);

        User drRivera = createUser("dr.rivera@smartdental.example.com", "Elena", "Rivera", "+1-555-0101", RoleName.ROLE_DENTIST);
        User drChen = createUser("dr.chen@smartdental.example.com", "Marcus", "Chen", "+1-555-0102", RoleName.ROLE_DENTIST);
        User hygienistLee = createUser("jamie.lee@smartdental.example.com", "Jamie", "Lee", "+1-555-0103", RoleName.ROLE_HYGIENIST);

        User patientNguyen = createUser("sofia.nguyen@example.com", "Sofia", "Nguyen", "+1-555-0201", RoleName.ROLE_PATIENT);
        User patientKovac = createUser("daniel.kovac@example.com", "Daniel", "Kovac", "+1-555-0202", RoleName.ROLE_PATIENT);
        User patientAdeyemi = createUser("grace.adeyemi@example.com", "Grace", "Adeyemi", "+1-555-0203", RoleName.ROLE_PATIENT);

        PatientProfile nguyenProfile = seedPatientProfile(
                patientNguyen, LocalDate.of(1994, 3, 12), BloodType.O_POSITIVE,
                Set.of("Penicillin allergy"), "Delta Dental PPO", "DD-88213", 80);
        PatientProfile kovacProfile = seedPatientProfile(
                patientKovac, LocalDate.of(1988, 11, 2), BloodType.A_NEGATIVE,
                Set.of("Latex allergy"), "Cigna Dental", "CG-40217", 70);
        PatientProfile adeyemiProfile = seedPatientProfile(
                patientAdeyemi, LocalDate.of(2001, 7, 22), BloodType.B_POSITIVE,
                Set.of(), "MetLife Dental", "ML-91820", 90);

        seedOdontogram(nguyenProfile, drRivera);
        seedOdontogram(kovacProfile, drChen);
        seedOdontogram(adeyemiProfile, drRivera);

        Instant now = Instant.now();
        Appointment upcomingConfirmed = seedAppointment(
                patientNguyen, drRivera, now.plus(2, ChronoUnit.DAYS), 60,
                "Routine cleaning and checkup appointment", AppointmentStatus.CONFIRMED);
        Appointment upcomingPending = seedAppointment(
                patientKovac, drChen, now.plus(4, ChronoUnit.DAYS), 45,
                "Tooth pain evaluation", AppointmentStatus.PENDING);
        Appointment pastCompleted = seedAppointment(
                patientNguyen, drRivera, now.minus(30, ChronoUnit.DAYS), 90,
                "Composite filling on #14", AppointmentStatus.COMPLETED);
        seedAppointment(
                patientAdeyemi, drRivera, now.plus(7, ChronoUnit.DAYS), 30,
                "Follow-up consultation", AppointmentStatus.PENDING);

        seedClinicalNote(
                patientNguyen, drRivera, pastCompleted, 14,
                "Tooth #14 DO composite, deep caries. Local anesthesia, no complications.",
                "D2394",
                "We treated a cavity on the back-left lower tooth using a tooth-colored filling. "
                        + "The procedure went smoothly with local numbing, and there were no complications. "
                        + "Mild sensitivity for a few days is normal.",
                true);
        seedClinicalNote(
                patientKovac, drChen, null, 30,
                "Tooth #30 MOD amalgam failing, recommend crown. Patient reports intermittent sharp pain.",
                "D2740",
                null,
                false);

        seedInvoice(patientNguyen, pastCompleted, InvoiceStatus.PARTIAL);
        seedInvoice(patientKovac, null, InvoiceStatus.UNPAID);

        log.info(
                "Seed complete: {} users, {} appointments, {} clinical notes, {} invoices",
                userRepository.count(),
                appointmentRepository.count(),
                clinicalNoteRepository.count(),
                invoiceRepository.count());
        log.info(
                "Demo login: {} / Password123! (all seeded accounts share this password)", admin.getEmail());
    }

    private User createUser(String email, String firstName, String lastName, String phone, RoleName role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(true);
        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }

    private PatientProfile seedPatientProfile(
            User user,
            LocalDate dob,
            BloodType bloodType,
            Set<String> alerts,
            String insurer,
            String policyNumber,
            int coveragePercent) {
        PatientProfile profile = new PatientProfile();
        profile.setUser(user);
        profile.setDateOfBirth(dob);
        profile.setBloodType(bloodType);
        profile.setMedicalAlerts(alerts);
        profile.setInsuranceProvider(insurer);
        profile.setInsurancePolicyNumber(policyNumber);
        profile.setInsuranceCoveragePercent(coveragePercent);
        profile.setEmergencyContactName("Jordan " + user.getLastName());
        profile.setEmergencyContactPhone("+1-555-0199");
        return patientProfileRepository.save(profile);
    }

    private void seedOdontogram(PatientProfile profile, User dentist) {
        for (int quadrant = 1; quadrant <= 4; quadrant++) {
            for (int tooth = 1; tooth <= 8; tooth++) {
                int toothNumber = quadrant * 10 + tooth;
                ToothRecord record = new ToothRecord();
                record.setPatientProfile(profile);
                record.setToothNumber(toothNumber);
                record.setRecordedBy(dentist);

                if (toothNumber == 14) {
                    record.setCondition(ToothCondition.FILLED);
                    record.setSurfaces(EnumSet.of(ToothSurface.DISTAL, ToothSurface.OCCLUSAL));
                    record.setNotes("DO composite, placed after seed appointment");
                } else if (toothNumber == 30) {
                    record.setCondition(ToothCondition.CARIES);
                    record.setSurfaces(EnumSet.of(ToothSurface.MESIAL, ToothSurface.OCCLUSAL, ToothSurface.DISTAL));
                    record.setNotes("MOD decay observed, crown recommended");
                } else if (toothNumber == 1 || toothNumber == 16 || toothNumber == 17 || toothNumber == 32) {
                    record.setCondition(ToothCondition.MISSING);
                } else {
                    record.setCondition(ToothCondition.HEALTHY);
                }

                toothRecordRepository.save(record);
            }
        }
    }

    private Appointment seedAppointment(
            User patient, User dentist, Instant start, int durationMinutes, String reason, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDentist(dentist);
        appointment.setStartTime(start);
        appointment.setEndTime(start.plus(durationMinutes, ChronoUnit.MINUTES));
        appointment.setReason(reason);
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    private void seedClinicalNote(
            User patient,
            User dentist,
            Appointment appointment,
            int toothNumber,
            String shorthand,
            String cdtCode,
            String aiSummary,
            boolean visible) {
        ClinicalNote note = new ClinicalNote();
        note.setPatient(patient);
        note.setDentist(dentist);
        note.setAppointment(appointment);
        note.setToothNumber(toothNumber);
        note.setShorthand(shorthand);
        note.setCdtCode(cdtCode);
        note.setAiSummary(aiSummary);
        note.setPatientVisible(visible);
        clinicalNoteRepository.save(note);
    }

    private void seedInvoice(User patient, Appointment appointment, InvoiceStatus status) {
        Invoice invoice = new Invoice();
        invoice.setPatient(patient);
        invoice.setAppointment(appointment);
        invoice.setStatus(status);
        invoice.setDueDate(LocalDate.now().plusDays(30));

        InvoiceLineItem procedure = new InvoiceLineItem();
        procedure.setInvoice(invoice);
        procedure.setDescription("Composite filling - 2 surface");
        procedure.setCdtCode("D2394");
        procedure.setQuantity(1);
        procedure.setUnitPrice(new BigDecimal("245.00"));
        procedure.setInsuranceCoveredAmount(new BigDecimal("196.00"));
        invoice.getLineItems().add(procedure);

        InvoiceLineItem exam = new InvoiceLineItem();
        exam.setInvoice(invoice);
        exam.setDescription("Periodic oral evaluation");
        exam.setCdtCode("D0120");
        exam.setQuantity(1);
        exam.setUnitPrice(new BigDecimal("65.00"));
        exam.setInsuranceCoveredAmount(new BigDecimal("52.00"));
        invoice.getLineItems().add(exam);

        invoiceRepository.save(invoice);
    }
}

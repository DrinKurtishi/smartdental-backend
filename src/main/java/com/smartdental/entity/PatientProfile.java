package com.smartdental.entity;

import com.smartdental.entity.enums.BloodType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patient_profiles")
@Getter
@Setter
@NoArgsConstructor
public class PatientProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", length = 20)
    private BloodType bloodType = BloodType.UNKNOWN;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "insurance_provider")
    private String insuranceProvider;

    @Column(name = "insurance_policy_number")
    private String insurancePolicyNumber;

    @Column(name = "insurance_group_number")
    private String insuranceGroupNumber;

    @Column(name = "insurance_coverage_percent")
    private Integer insuranceCoveragePercent;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "patient_medical_alerts", joinColumns = @JoinColumn(name = "patient_profile_id"))
    @Column(name = "alert", nullable = false)
    private Set<String> medicalAlerts = new HashSet<>();

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;
}

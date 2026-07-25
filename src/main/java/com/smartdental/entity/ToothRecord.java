package com.smartdental.entity;

import com.smartdental.entity.enums.ToothCondition;
import com.smartdental.entity.enums.ToothSurface;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row per FDI tooth number (11-48) capturing the current odontogram state for a patient. */
@Entity
@Table(
        name = "tooth_records",
        indexes = {@Index(name = "idx_tooth_records_patient", columnList = "patient_profile_id")},
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_tooth_records_patient_tooth",
                    columnNames = {"patient_profile_id", "tooth_number"})
        })
@Getter
@Setter
@NoArgsConstructor
public class ToothRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_profile_id", nullable = false)
    private PatientProfile patientProfile;

    /** FDI two-digit notation, 11-48. */
    @Column(name = "tooth_number", nullable = false)
    private int toothNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_status", nullable = false, length = 30)
    private ToothCondition condition = ToothCondition.HEALTHY;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tooth_record_surfaces", joinColumns = @JoinColumn(name = "tooth_record_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "surface", nullable = false, length = 20)
    private Set<ToothSurface> surfaces = EnumSet.noneOf(ToothSurface.class);

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    private User recordedBy;
}

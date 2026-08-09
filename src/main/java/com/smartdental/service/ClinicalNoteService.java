package com.smartdental.service;

import com.smartdental.dto.clinicalnote.ClinicalNoteCreateRequest;
import com.smartdental.dto.clinicalnote.ClinicalNoteResponse;
import com.smartdental.entity.Appointment;
import com.smartdental.entity.ClinicalNote;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.RoleName;
import com.smartdental.exception.ResourceNotFoundException;
import com.smartdental.repository.AppointmentRepository;
import com.smartdental.repository.ClinicalNoteRepository;
import com.smartdental.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicalNoteService {

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ClinicalNoteResponse> findForPatient(UUID patientId, boolean visibleOnly) {
        List<ClinicalNote> notes =
                visibleOnly
                        ? clinicalNoteRepository.findByPatientIdAndPatientVisibleTrueOrderByCreatedAtDesc(patientId)
                        : clinicalNoteRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return notes.stream().map(ClinicalNoteResponse::from).toList();
    }

    @Transactional
    public ClinicalNoteResponse create(ClinicalNoteCreateRequest request, UUID dentistId) {
        User patient =
                userRepository
                        .findById(request.patientId())
                        .filter(u -> u.getRoles().contains(RoleName.ROLE_PATIENT))
                        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.patientId()));
        User dentist =
                userRepository
                        .findById(dentistId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dentist not found: " + dentistId));

        ClinicalNote note = new ClinicalNote();
        note.setPatient(patient);
        note.setDentist(dentist);
        note.setShorthand(request.shorthand());
        note.setAiSummary(request.aiSummary());
        note.setCdtCode(request.cdtCode());
        note.setToothNumber(request.toothNumber());
        note.setPatientVisible(request.patientVisible());

        if (request.appointmentId() != null) {
            Appointment appointment =
                    appointmentRepository
                            .findById(request.appointmentId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Appointment not found: " + request.appointmentId()));
            note.setAppointment(appointment);
        }

        return ClinicalNoteResponse.from(clinicalNoteRepository.save(note));
    }

    @Transactional
    public ClinicalNoteResponse setAiSummary(UUID noteId, String aiSummary) {
        ClinicalNote note = getOrThrow(noteId);
        note.setAiSummary(aiSummary);
        return ClinicalNoteResponse.from(clinicalNoteRepository.save(note));
    }

    @Transactional
    public ClinicalNoteResponse setVisibility(UUID noteId, boolean visible) {
        ClinicalNote note = getOrThrow(noteId);
        note.setPatientVisible(visible);
        return ClinicalNoteResponse.from(clinicalNoteRepository.save(note));
    }

    private ClinicalNote getOrThrow(UUID id) {
        return clinicalNoteRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinical note not found: " + id));
    }
}

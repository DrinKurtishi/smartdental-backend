package com.smartdental.service;

import com.smartdental.dto.odontogram.ToothRecordResponse;
import com.smartdental.dto.odontogram.ToothRecordUpsertRequest;
import com.smartdental.entity.PatientProfile;
import com.smartdental.entity.ToothRecord;
import com.smartdental.entity.User;
import com.smartdental.entity.enums.ToothCondition;
import com.smartdental.entity.enums.ToothSurface;
import com.smartdental.exception.BadRequestException;
import com.smartdental.exception.ResourceNotFoundException;
import com.smartdental.repository.ToothRecordRepository;
import com.smartdental.repository.UserRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OdontogramService {

    /** FDI two-digit tooth numbers span the four quadrants 11-18, 21-28, 31-38, 41-48. */
    private static final Set<Integer> VALID_FDI_NUMBERS = buildValidFdiNumbers();

    private static Set<Integer> buildValidFdiNumbers() {
        java.util.Set<Integer> numbers = new java.util.HashSet<>();
        for (int quadrant = 1; quadrant <= 4; quadrant++) {
            for (int tooth = 1; tooth <= 8; tooth++) {
                numbers.add(quadrant * 10 + tooth);
            }
        }
        return numbers;
    }

    private final PatientService patientService;
    private final ToothRecordRepository toothRecordRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ToothRecordResponse> getOdontogram(UUID patientUserId) {
        PatientProfile profile = patientService.getOrCreateProfile(patientUserId);
        return toothRecordRepository.findByPatientProfileIdOrderByToothNumberAsc(profile.getId()).stream()
                .map(ToothRecordResponse::from)
                .toList();
    }

    @Transactional
    public ToothRecordResponse upsertTooth(
            UUID patientUserId, int toothNumber, ToothRecordUpsertRequest request, UUID recordedByUserId) {
        if (!VALID_FDI_NUMBERS.contains(toothNumber)) {
            throw new BadRequestException("Invalid FDI tooth number: " + toothNumber);
        }

        PatientProfile profile = patientService.getOrCreateProfile(patientUserId);
        ToothRecord record =
                toothRecordRepository
                        .findByPatientProfileIdAndToothNumber(profile.getId(), toothNumber)
                        .orElseGet(
                                () -> {
                                    ToothRecord newRecord = new ToothRecord();
                                    newRecord.setPatientProfile(profile);
                                    newRecord.setToothNumber(toothNumber);
                                    return newRecord;
                                });

        record.setCondition(parseCondition(request.condition()));
        record.setSurfaces(parseSurfaces(request.surfaces()));
        record.setNotes(request.notes());

        if (recordedByUserId != null) {
            User recordedBy =
                    userRepository
                            .findById(recordedByUserId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + recordedByUserId));
            record.setRecordedBy(recordedBy);
        }

        return ToothRecordResponse.from(toothRecordRepository.save(record));
    }

    private ToothCondition parseCondition(String raw) {
        try {
            return ToothCondition.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown tooth condition: " + raw);
        }
    }

    private Set<ToothSurface> parseSurfaces(Set<String> raw) {
        return raw.stream()
                .map(
                        s -> {
                            try {
                                return ToothSurface.valueOf(s.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                throw new BadRequestException("Unknown tooth surface: " + s);
                            }
                        })
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ToothSurface.class)));
    }
}

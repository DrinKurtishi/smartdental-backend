package com.smartdental.dto.odontogram;

import com.smartdental.entity.ToothRecord;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ToothRecordResponse(
        UUID id,
        int toothNumber,
        String condition,
        Set<String> surfaces,
        String notes,
        String recordedByName,
        Instant updatedAt) {

    public static ToothRecordResponse from(ToothRecord record) {
        return new ToothRecordResponse(
                record.getId(),
                record.getToothNumber(),
                record.getCondition().name(),
                record.getSurfaces().stream().map(Enum::name).collect(Collectors.toSet()),
                record.getNotes(),
                record.getRecordedBy() != null ? record.getRecordedBy().getFullName() : null,
                record.getUpdatedAt());
    }
}

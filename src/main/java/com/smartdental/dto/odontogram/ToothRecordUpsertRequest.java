package com.smartdental.dto.odontogram;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record ToothRecordUpsertRequest(
        @NotBlank String condition, Set<String> surfaces, String notes) {

    public ToothRecordUpsertRequest {
        if (surfaces == null) {
            surfaces = Set.of();
        }
    }
}

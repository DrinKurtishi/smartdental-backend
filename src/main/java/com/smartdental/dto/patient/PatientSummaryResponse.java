package com.smartdental.dto.patient;

import com.smartdental.entity.User;
import java.util.UUID;

public record PatientSummaryResponse(UUID userId, String fullName, String email, String phone) {

    public static PatientSummaryResponse from(User user) {
        return new PatientSummaryResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone());
    }
}

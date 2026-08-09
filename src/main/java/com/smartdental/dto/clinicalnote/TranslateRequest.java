package com.smartdental.dto.clinicalnote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TranslateRequest(@NotBlank @Size(max = 2000) String shorthand) {}

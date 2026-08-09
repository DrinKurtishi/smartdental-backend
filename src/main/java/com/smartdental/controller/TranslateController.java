package com.smartdental.controller;

import com.smartdental.dto.clinicalnote.TranslateRequest;
import com.smartdental.dto.clinicalnote.TranslateResponse;
import com.smartdental.service.translation.TranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/translate")
@RequiredArgsConstructor
public class TranslateController {

    private final TranslationService translationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'HYGIENIST')")
    public TranslateResponse translate(@Valid @RequestBody TranslateRequest request) {
        return translationService.translate(request.shorthand());
    }
}

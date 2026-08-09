package com.smartdental.service.translation;

import com.smartdental.config.AiTranslationProperties;
import com.smartdental.dto.clinicalnote.TranslateResponse;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TranslationService {

    private final AiTranslationProperties properties;
    private final Map<String, AiTranslationClient> clientsByProvider;

    public TranslationService(AiTranslationProperties properties, Map<String, AiTranslationClient> clientsByProvider) {
        this.properties = properties;
        this.clientsByProvider = clientsByProvider;
    }

    public TranslateResponse translate(String shorthand) {
        if (properties.enabled() && properties.apiKey() != null && !properties.apiKey().isBlank()) {
            AiTranslationClient client = clientsByProvider.get(properties.provider());
            if (client != null) {
                Optional<String> aiSummary = client.generateSummary(shorthand);
                if (aiSummary.isPresent()) {
                    return new TranslateResponse(aiSummary.get(), true);
                }
            } else {
                log.warn("No translation client registered for provider '{}'", properties.provider());
            }
        }

        return new TranslateResponse(FallbackSummaryGenerator.generate(shorthand), false);
    }
}

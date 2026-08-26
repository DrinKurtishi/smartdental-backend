package com.smartdental.service.translation;

import com.smartdental.config.AiTranslationProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component("gemini")
public class GeminiTranslationClient implements AiTranslationClient {

    private static final String PROMPT_PREFIX =
            "Rewrite this dentist's clinical shorthand into a short, reassuring, plain-English summary "
                    + "for a patient with no dental background. Keep it to 2-3 sentences, avoid jargon and "
                    + "procedure codes. Shorthand: ";

    private final AiTranslationProperties properties;
    private final WebClient webClient;

    public GeminiTranslationClient(AiTranslationProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder().baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Optional<String> generateSummary(String shorthand) {
        try {
            Map<String, Object> body =
                    Map.of(
                            "contents",
                            List.of(Map.of("parts", List.of(Map.of("text", PROMPT_PREFIX + shorthand)))));

            Map<?, ?> response =
                    webClient
                            .post()
                            .uri(
                                    uriBuilder ->
                                            uriBuilder
                                                    .path("/models/{model}:generateContent")
                                                    .build(properties.model()))
                            .header("x-goog-api-key", properties.apiKey())
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block(Duration.ofMillis(properties.timeoutMs()));

            return extractContent(response);
        } catch (Exception e) {
            log.warn("Gemini translation call failed, falling back to rule-based summary: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<String> extractContent(Map<?, ?> response) {
        if (response == null) {
            return Optional.empty();
        }
        Object candidatesObj = response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            return Optional.empty();
        }
        Object first = candidates.get(0);
        if (!(first instanceof Map<?, ?> candidate)) {
            return Optional.empty();
        }
        Object contentObj = candidate.get("content");
        if (!(contentObj instanceof Map<?, ?> content)) {
            return Optional.empty();
        }
        Object partsObj = content.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return Optional.empty();
        }
        Object firstPart = parts.get(0);
        if (!(firstPart instanceof Map<?, ?> part)) {
            return Optional.empty();
        }
        Object text = part.get("text");
        return text instanceof String s && !s.isBlank() ? Optional.of(s.trim()) : Optional.empty();
    }
}

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
@Component("openai")
public class OpenAiTranslationClient implements AiTranslationClient {

    private static final String SYSTEM_PROMPT =
            "You are a dental assistant that rewrites a dentist's clinical shorthand into a short, "
                    + "reassuring, plain-English summary a patient with no dental background can understand. "
                    + "Keep it to 2-3 sentences, avoid jargon and procedure codes, and do not invent facts "
                    + "that are not implied by the shorthand.";

    private final AiTranslationProperties properties;
    private final WebClient webClient;

    public OpenAiTranslationClient(AiTranslationProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder().baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Optional<String> generateSummary(String shorthand) {
        try {
            Map<String, Object> body =
                    Map.of(
                            "model", properties.model(),
                            "temperature", 0.4,
                            "messages",
                                    List.of(
                                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                                            Map.of("role", "user", "content", shorthand)));

            Map<?, ?> response =
                    webClient
                            .post()
                            .uri("/chat/completions")
                            .header("Authorization", "Bearer " + properties.apiKey())
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block(Duration.ofMillis(properties.timeoutMs()));

            return extractContent(response);
        } catch (Exception e) {
            log.warn("OpenAI translation call failed, falling back to rule-based summary: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<String> extractContent(Map<?, ?> response) {
        if (response == null) {
            return Optional.empty();
        }
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return Optional.empty();
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return Optional.empty();
        }
        Object messageObj = choice.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return Optional.empty();
        }
        Object content = message.get("content");
        return content instanceof String text && !text.isBlank() ? Optional.of(text.trim()) : Optional.empty();
    }
}

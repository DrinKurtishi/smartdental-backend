package com.smartdental.service.translation;

import java.util.Optional;

/** A provider that turns dentist shorthand into a plain-English patient summary. */
public interface AiTranslationClient {

    /** Returns empty when the provider is unreachable, mis-configured, or errors — callers should fall back. */
    Optional<String> generateSummary(String shorthand);
}

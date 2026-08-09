package com.smartdental.service.translation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rule-based plain-English summary used when no AI provider is reachable. */
public final class FallbackSummaryGenerator {

    private static final Pattern TOOTH_PATTERN = Pattern.compile("(?i)tooth\\s*#?\\s*(\\d{1,2})");

    private static final Map<String, String> TERMS = new LinkedHashMap<>();

    static {
        TERMS.put("mod", "a filling covering three sides of the tooth");
        TERMS.put("do", "a filling on two sides of the tooth");
        TERMS.put("occlusal", "the chewing surface");
        TERMS.put("caries", "a cavity");
        TERMS.put("composite", "a tooth-colored filling");
        TERMS.put("amalgam", "a silver filling");
        TERMS.put("crown", "a protective cap");
        TERMS.put("root canal", "a procedure to treat the tooth's inner nerve");
        TERMS.put("rct", "a root canal treatment");
        TERMS.put("extraction", "removal of the tooth");
        TERMS.put("scaling", "a deep dental cleaning");
        TERMS.put("periapical", "around the tip of the tooth's root");
        TERMS.put("bridge", "a fixed replacement for a missing tooth");
        TERMS.put("implant", "an artificial tooth root");
    }

    private FallbackSummaryGenerator() {}

    public static String generate(String shorthand) {
        String lower = shorthand.toLowerCase();
        StringBuilder plainTerms = new StringBuilder();

        for (Map.Entry<String, String> entry : TERMS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                if (!plainTerms.isEmpty()) {
                    plainTerms.append(", ");
                }
                plainTerms.append(entry.getValue());
            }
        }

        Matcher toothMatcher = TOOTH_PATTERN.matcher(shorthand);
        String toothClause = toothMatcher.find() ? " on tooth #" + toothMatcher.group(1) : "";

        if (plainTerms.isEmpty()) {
            return "Your dentist recorded a clinical note" + toothClause + ". Ask your care team for details "
                    + "in your next visit if you'd like a fuller explanation.";
        }

        return "Your dentist performed or noted " + plainTerms + toothClause
                + ". This is a routine part of your dental care.";
    }
}

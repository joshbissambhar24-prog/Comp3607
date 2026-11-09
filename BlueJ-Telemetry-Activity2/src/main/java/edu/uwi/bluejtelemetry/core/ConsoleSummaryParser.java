package edu.uwi.bluejtelemetry.core;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Summarises console text intentionally pasted by a user, never intercepts System.out. */
public final class ConsoleSummaryParser {
    public Map<String,Object> summarize(String pastedOutput) {
        String text = pastedOutput == null ? "" : pastedOutput;
        String[] lines = text.isEmpty() ? new String[0] : text.split("\\R", -1);
        int nonBlank = 0, errorLines = 0;
        for (String line : lines) {
            if (!line.isBlank()) nonBlank++;
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("exception") || lower.contains("error")) errorLines++;
        }
        Map<String,Object> summary = new LinkedHashMap<>();
        summary.put("collection_method", "manual_console_copy_paste");
        summary.put("character_count", text.length());
        summary.put("non_blank_line_count", nonBlank);
        summary.put("exception_or_error_line_count", errorLines);
        summary.put("first_line_excerpt", firstNonBlankLine(lines));
        summary.put("contains_unverified_user_pasted_text", true);
        return summary;
    }
    private String firstNonBlankLine(String[] lines) {
        for (String line : lines) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) return trimmed.length() <= 100 ? trimmed : trimmed.substring(0,97) + "...";
        }
        return "";
    }
}

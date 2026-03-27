package com.incident.classifier.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits raw text into chunks (paragraphs or sentences).
 *
 * Paragraph mode  – splits on one or more blank lines.
 * Sentence mode   – splits on sentence-ending punctuation (. ! ?) followed by whitespace.
 */
@Component
public class TextChunker {

    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\\n\\s*\\n+");
    private static final Pattern SENTENCE_SPLIT   = Pattern.compile("(?<=[.!?])\\s+");

    /**
     * @param text      raw input text
     * @param chunkMode "paragraph" (default) or "sentence"
     * @return non-empty, trimmed chunks
     */
    public List<String> chunk(String text, String chunkMode) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String[] parts;
        if ("sentence".equalsIgnoreCase(chunkMode)) {
            parts = SENTENCE_SPLIT.split(text.trim());
        } else {
            // paragraph is the default
            parts = PARAGRAPH_SPLIT.split(text.trim());
        }

        List<String> chunks = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                chunks.add(trimmed);
            }
        }

        // If no split occurred (single block of text) and paragraph mode was chosen,
        // fall back to sentence-splitting so we always return more than one unit.
        if (chunks.size() == 1 && !"sentence".equalsIgnoreCase(chunkMode)) {
            return chunk(text, "sentence");
        }

        return chunks;
    }
}

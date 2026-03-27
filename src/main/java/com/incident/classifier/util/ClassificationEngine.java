package com.incident.classifier.util;

import com.incident.classifier.model.Topic;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Core classification logic.
 *
 * Scoring algorithm (per chunk × per topic):
 *  1. Exact keyword match        → +1.0 per keyword occurrence
 *  2. Case-insensitive match      → same weight (handled by toLowerCase)
 *  3. Partial / substring match   → +0.5 (keyword appears inside a larger token)
 *  4. Levenshtein distance ≤ 1    → +0.3 (simple typo tolerance)
 *
 * Confidence = topicScore / maxPossibleScore (clipped to [0,1]).
 */
@Component
public class ClassificationEngine {

    public static final String UNCLASSIFIED = "UNCLASSIFIED";

    public record ClassificationResult(
            Topic topic,
            double confidenceScore,
            boolean unclassified,
            boolean ambiguous
    ) {}

    /**
     * Classifies a single text chunk against all available topics.
     */
    public ClassificationResult classify(String chunk, List<Topic> topics) {
        if (topics == null || topics.isEmpty()) {
            return new ClassificationResult(null, 0.0, true, false);
        }

        String normalised = chunk.toLowerCase(Locale.ROOT);
        String[] tokens = normalised.split("\\W+");

        Map<Topic, Double> scores = new LinkedHashMap<>();
        for (Topic topic : topics) {
            double score = computeScore(normalised, tokens, topic);
            scores.put(topic, score);
        }

        double maxScore = scores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        if (maxScore == 0.0) {
            return new ClassificationResult(null, 0.0, true, false);
        }

        // collect all topics that share the top score
        List<Topic> topTopics = scores.entrySet().stream()
                .filter(e -> e.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .toList();

        boolean ambiguous = topTopics.size() > 1;
        Topic winner = topTopics.get(0);

        // max possible score = total keywords * 1.0 (exact match weight)
        double maxPossible = winner.getKeywords().size();
        double confidence = maxPossible > 0
                ? Math.min(maxScore / maxPossible, 1.0)
                : 0.0;

        return new ClassificationResult(winner, confidence, false, ambiguous);
    }

    // ─── Scoring ──────────────────────────────────────────────────────────────

    private double computeScore(String normalised, String[] tokens, Topic topic) {
        double score = 0.0;
        for (String keyword : topic.getKeywords()) {
            String kw = keyword.toLowerCase(Locale.ROOT);
            // 1. Exact / full-word match (includes case-insensitive)
            if (normalised.contains(kw)) {
                // Count occurrences
                int count = countOccurrences(normalised, kw);
                score += count * 1.0;
                continue;
            }
            // 2. Partial token match (substring)
            boolean partialMatched = false;
            for (String token : tokens) {
                if (token.contains(kw) || kw.contains(token)) {
                    score += 0.5;
                    partialMatched = true;
                    break;
                }
            }
            if (partialMatched) continue;
            // 3. Levenshtein fuzzy match (typo tolerance)
            for (String token : tokens) {
                if (levenshtein(token, kw) <= 1 && !token.isEmpty() && !kw.isEmpty()) {
                    score += 0.3;
                    break;
                }
            }
        }
        return score;
    }

    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }

    /**
     * Classic iterative Levenshtein distance — O(m×n) time, O(min(m,n)) space.
     */
    private int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[] dp = new int[n + 1];
        for (int j = 0; j <= n; j++) dp[j] = j;
        for (int i = 1; i <= m; i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= n; j++) {
                int temp = dp[j];
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[j] = prev;
                } else {
                    dp[j] = 1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
                }
                prev = temp;
            }
        }
        return dp[n];
    }
}

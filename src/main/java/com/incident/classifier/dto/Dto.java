package com.incident.classifier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

// ─── Request DTOs ──────────────────────────────────────────────────────────────

public class Dto {

    @Data
    public static class TopicRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotEmpty(message = "At least one keyword is required")
        private List<@NotBlank String> keywords;
    }

    @Data
    public static class TextDocumentRequest {
        @NotBlank(message = "Text content is required")
        private String text;

        /** paragraph (default) or sentence */
        private String chunkMode = "paragraph";
    }

    // ─── Response DTOs ─────────────────────────────────────────────────────────

    @Data
    public static class TopicResponse {
        private Long id;
        private String title;
        private List<String> keywords;
    }

    @Data
    public static class DocumentResponse {
        private Long id;
        private String fileName;
        private String sourceType;
        private String status;
        private String createdAt;
        private int totalChunks;
    }

    @Data
    public static class ChunkResult {
        private Long id;
        private String text;
        private String assignedTopic;
        private Double confidence;
        private boolean unclassified;
        private boolean ambiguous;
        private int chunkIndex;
    }

    @Data
    public static class ClassificationResultResponse {
        private Long documentId;
        private String status;
        private int totalChunks;
        private List<ChunkResult> results;
    }

    @Data
    public static class DashboardResponse {
        private long totalDocuments;
        private long totalChunks;
        private java.util.Map<String, Long> topicDistribution;
    }

    @Data
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private String timestamp;

        public ErrorResponse(int status, String error, String message) {
            this.status = status;
            this.error = error;
            this.message = message;
            this.timestamp = java.time.LocalDateTime.now().toString();
        }
    }
}

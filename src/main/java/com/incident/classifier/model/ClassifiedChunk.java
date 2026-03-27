package com.incident.classifier.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "classified_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifiedChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "text_chunk", columnDefinition = "TEXT", nullable = false)
    private String textChunk;

    /**
     * Null if UNCLASSIFIED
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_topic_id")
    private Topic assignedTopic;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    /**
     * True when no topic matched (score = 0)
     */
    @Column(name = "is_unclassified")
    @Builder.Default
    private Boolean isUnclassified = false;

    /**
     * True when two or more topics tie for the top score
     */
    @Column(name = "is_ambiguous")
    @Builder.Default
    private Boolean isAmbiguous = false;

    @Column(name = "chunk_index")
    private Integer chunkIndex;
}

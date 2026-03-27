package com.incident.classifier.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "topics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    /**
     * Keywords stored as a comma-separated string in DB.
     * Exposed as a List<String> via @ElementCollection for flexibility.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "topic_keywords", joinColumns = @JoinColumn(name = "topic_id"))
    @Column(name = "keyword")
    private List<String> keywords;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

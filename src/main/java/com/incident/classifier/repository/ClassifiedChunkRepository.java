package com.incident.classifier.repository;

import com.incident.classifier.model.ClassifiedChunk;
import com.incident.classifier.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassifiedChunkRepository extends JpaRepository<ClassifiedChunk, Long> {

    List<ClassifiedChunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    Page<ClassifiedChunk> findByDocumentId(Long documentId, Pageable pageable);

    long countByDocumentId(Long documentId);

    /**
     * Count chunks per topic title for dashboard aggregation.
     * UNCLASSIFIED chunks (assignedTopic = null) are counted separately.
     */
    @Query("SELECT c.assignedTopic, COUNT(c) FROM ClassifiedChunk c GROUP BY c.assignedTopic")
    List<Object[]> countGroupedByTopic();

    @Query("SELECT COUNT(c) FROM ClassifiedChunk c WHERE c.isUnclassified = true")
    long countUnclassified();

    @Query("SELECT COUNT(c) FROM ClassifiedChunk c WHERE c.assignedTopic = :topic")
    long countByAssignedTopic(@Param("topic") Topic topic);
}

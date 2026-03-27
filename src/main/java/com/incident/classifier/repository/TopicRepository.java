package com.incident.classifier.repository;

import com.incident.classifier.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    Optional<Topic> findByTitleIgnoreCase(String title);
    boolean existsByTitleIgnoreCase(String title);
}

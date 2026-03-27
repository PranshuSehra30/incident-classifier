package com.incident.classifier.service;

import com.incident.classifier.dto.Dto;
import com.incident.classifier.exception.DuplicateResourceException;
import com.incident.classifier.exception.ResourceNotFoundException;
import com.incident.classifier.model.Topic;
import com.incident.classifier.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    @Transactional
    public Dto.TopicResponse createTopic(Dto.TopicRequest request) {
        if (topicRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new DuplicateResourceException(
                    "A topic with title '" + request.getTitle() + "' already exists.");
        }

        // Normalise keywords: lowercase, trim, deduplicate
        List<String> keywords = request.getKeywords().stream()
                .map(k -> k.trim().toLowerCase())
                .distinct()
                .collect(Collectors.toList());

        Topic topic = Topic.builder()
                .title(request.getTitle().trim())
                .keywords(keywords)
                .build();

        Topic saved = topicRepository.save(topic);
        log.info("Created topic [id={}] '{}'", saved.getId(), saved.getTitle());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Dto.TopicResponse> getAllTopics() {
        return topicRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Dto.TopicResponse getTopicById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public Dto.TopicResponse updateTopic(Long id, Dto.TopicRequest request) {
        Topic topic = findOrThrow(id);

        // Check title uniqueness only if title is changing
        if (!topic.getTitle().equalsIgnoreCase(request.getTitle())
                && topicRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new DuplicateResourceException(
                    "A topic with title '" + request.getTitle() + "' already exists.");
        }

        List<String> keywords = request.getKeywords().stream()
                .map(k -> k.trim().toLowerCase())
                .distinct()
                .collect(Collectors.toList());

        topic.setTitle(request.getTitle().trim());
        topic.setKeywords(keywords);

        Topic saved = topicRepository.save(topic);
        log.info("Updated topic [id={}]", id);
        return toResponse(saved);
    }

    @Transactional
    public void deleteTopic(Long id) {
        findOrThrow(id);
        topicRepository.deleteById(id);
        log.info("Deleted topic [id={}]", id);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public Topic findOrThrow(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));
    }

    private Dto.TopicResponse toResponse(Topic topic) {
        Dto.TopicResponse resp = new Dto.TopicResponse();
        resp.setId(topic.getId());
        resp.setTitle(topic.getTitle());
        resp.setKeywords(topic.getKeywords());
        return resp;
    }
}

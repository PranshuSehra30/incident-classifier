package com.incident.classifier.controller;

import com.incident.classifier.dto.Dto;
import com.incident.classifier.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "Manage incident classification topics")
public class TopicController {

    private final TopicService topicService;

    @PostMapping
    @Operation(summary = "Create a new topic with keywords")
    public ResponseEntity<Dto.TopicResponse> createTopic(
            @Valid @RequestBody Dto.TopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(topicService.createTopic(request));
    }

    @GetMapping
    @Operation(summary = "List all topics")
    public ResponseEntity<List<Dto.TopicResponse>> getAllTopics() {
        return ResponseEntity.ok(topicService.getAllTopics());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a topic by ID")
    public ResponseEntity<Dto.TopicResponse> getTopicById(@PathVariable Long id) {
        return ResponseEntity.ok(topicService.getTopicById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing topic")
    public ResponseEntity<Dto.TopicResponse> updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody Dto.TopicRequest request) {
        return ResponseEntity.ok(topicService.updateTopic(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a topic by ID")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}

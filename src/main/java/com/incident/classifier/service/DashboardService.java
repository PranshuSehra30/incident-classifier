package com.incident.classifier.service;

import com.incident.classifier.dto.Dto;
import com.incident.classifier.model.Topic;
import com.incident.classifier.repository.ClassifiedChunkRepository;
import com.incident.classifier.repository.DocumentRepository;
import com.incident.classifier.repository.TopicRepository;
import com.incident.classifier.util.ClassificationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DocumentRepository        documentRepository;
    private final ClassifiedChunkRepository chunkRepository;
    private final TopicRepository           topicRepository;

    @Transactional(readOnly = true)
    public Dto.DashboardResponse getDashboard() {
        long totalDocuments = documentRepository.count();
        long totalChunks    = chunkRepository.count();

        Map<String, Long> distribution = new LinkedHashMap<>();

        // Per-topic counts
        for (Topic topic : topicRepository.findAll()) {
            long count = chunkRepository.countByAssignedTopic(topic);
            distribution.put(topic.getTitle(), count);
        }

        // Unclassified count
        long unclassifiedCount = chunkRepository.countUnclassified();
        distribution.put(ClassificationEngine.UNCLASSIFIED, unclassifiedCount);

        Dto.DashboardResponse resp = new Dto.DashboardResponse();
        resp.setTotalDocuments(totalDocuments);
        resp.setTotalChunks(totalChunks);
        resp.setTopicDistribution(distribution);
        return resp;
    }
}

package com.incident.classifier.service;

import com.incident.classifier.dto.Dto;
import com.incident.classifier.exception.DocumentProcessingException;
import com.incident.classifier.exception.ResourceNotFoundException;
import com.incident.classifier.model.ClassifiedChunk;
import com.incident.classifier.model.Document;
import com.incident.classifier.model.Topic;
import com.incident.classifier.repository.ClassifiedChunkRepository;
import com.incident.classifier.repository.DocumentRepository;
import com.incident.classifier.repository.TopicRepository;
import com.incident.classifier.util.ClassificationEngine;
import com.incident.classifier.util.PdfExtractor;
import com.incident.classifier.util.TextChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository      documentRepository;
    private final ClassifiedChunkRepository chunkRepository;
    private final TopicRepository          topicRepository;
    private final PdfExtractor             pdfExtractor;
    private final TextChunker              textChunker;
    private final ClassificationEngine     classificationEngine;

    // ─── Upload PDF ───────────────────────────────────────────────────────────

    @Transactional
    public Dto.DocumentResponse uploadPdf(MultipartFile file, String chunkMode) {
        validatePdfFile(file);

        String text = pdfExtractor.extract(file);

        Document doc = Document.builder()
                .fileName(file.getOriginalFilename())
                .sourceType(Document.SourceType.PDF)
                .originalText(text)
                .status(Document.ProcessingStatus.PENDING)
                .build();

        doc = documentRepository.save(doc);
        log.info("Saved PDF document [id={}] '{}'", doc.getId(), doc.getFileName());

        return processAndClassify(doc, chunkMode);
    }

    // ─── Upload Raw Text ──────────────────────────────────────────────────────

    @Transactional
    public Dto.DocumentResponse uploadText(Dto.TextDocumentRequest request) {
        Document doc = Document.builder()
                .fileName("text-input")
                .sourceType(Document.SourceType.TEXT)
                .originalText(request.getText())
                .status(Document.ProcessingStatus.PENDING)
                .build();

        doc = documentRepository.save(doc);
        log.info("Saved text document [id={}]", doc.getId());

        return processAndClassify(doc, request.getChunkMode());
    }

    // ─── Core Processing Pipeline ─────────────────────────────────────────────

    private Dto.DocumentResponse processAndClassify(Document doc, String chunkMode) {
        doc.setStatus(Document.ProcessingStatus.PROCESSING);
        documentRepository.save(doc);

        try {
            List<Topic> topics = topicRepository.findAll();
            if (topics.isEmpty()) {
                log.warn("No topics defined — all chunks will be UNCLASSIFIED");
            }

            List<String> chunks = textChunker.chunk(doc.getOriginalText(), chunkMode);
            if (chunks.isEmpty()) {
                throw new DocumentProcessingException("Document produced no text chunks after processing.");
            }

            log.info("Document [id={}] split into {} chunks (mode={})",
                    doc.getId(), chunks.size(), chunkMode);

            List<ClassifiedChunk> classifiedChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                ClassificationEngine.ClassificationResult result =
                        classificationEngine.classify(chunkText, topics);

                ClassifiedChunk chunk = ClassifiedChunk.builder()
                        .document(doc)
                        .textChunk(chunkText)
                        .assignedTopic(result.topic())
                        .confidenceScore(result.confidenceScore())
                        .isUnclassified(result.unclassified())
                        .isAmbiguous(result.ambiguous())
                        .chunkIndex(i)
                        .build();

                classifiedChunks.add(chunk);
            }

            chunkRepository.saveAll(classifiedChunks);

            doc.setStatus(Document.ProcessingStatus.COMPLETED);
            documentRepository.save(doc);

            log.info("Document [id={}] classified successfully ({} chunks)", doc.getId(), chunks.size());

            Dto.DocumentResponse resp = toDocumentResponse(doc);
            resp.setTotalChunks(classifiedChunks.size());
            return resp;

        } catch (Exception e) {
            doc.setStatus(Document.ProcessingStatus.FAILED);
            documentRepository.save(doc);
            log.error("Classification failed for document [id={}]: {}", doc.getId(), e.getMessage());
            throw e instanceof DocumentProcessingException dpe ? dpe
                    : new DocumentProcessingException("Classification failed: " + e.getMessage(), e);
        }
    }

    // ─── Get Results ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Dto.ClassificationResultResponse getResults(Long documentId, Pageable pageable) {
        Document doc = findDocumentOrThrow(documentId);

        Page<ClassifiedChunk> page = chunkRepository.findByDocumentId(documentId, pageable);

        List<Dto.ChunkResult> results = page.getContent().stream()
                .map(this::toChunkResult)
                .collect(Collectors.toList());

        Dto.ClassificationResultResponse resp = new Dto.ClassificationResultResponse();
        resp.setDocumentId(documentId);
        resp.setStatus(doc.getStatus().name());
        resp.setTotalChunks((int) chunkRepository.countByDocumentId(documentId));
        resp.setResults(results);
        return resp;
    }

    // ─── List Documents ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Dto.DocumentResponse> listDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable).map(doc -> {
            Dto.DocumentResponse resp = toDocumentResponse(doc);
            resp.setTotalChunks((int) chunkRepository.countByDocumentId(doc.getId()));
            return resp;
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Document findDocumentOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentProcessingException("Uploaded file is empty.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new DocumentProcessingException(
                    "Invalid file type '" + contentType + "'. Only PDF files are accepted on this endpoint.");
        }
    }

    private Dto.DocumentResponse toDocumentResponse(Document doc) {
        Dto.DocumentResponse resp = new Dto.DocumentResponse();
        resp.setId(doc.getId());
        resp.setFileName(doc.getFileName());
        resp.setSourceType(doc.getSourceType().name());
        resp.setStatus(doc.getStatus().name());
        resp.setCreatedAt(doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null);
        return resp;
    }

    private Dto.ChunkResult toChunkResult(ClassifiedChunk chunk) {
        Dto.ChunkResult result = new Dto.ChunkResult();
        result.setId(chunk.getId());
        result.setText(chunk.getTextChunk());
        result.setAssignedTopic(
                chunk.getIsUnclassified() ? ClassificationEngine.UNCLASSIFIED
                        : chunk.getAssignedTopic().getTitle());
        result.setConfidence(chunk.getConfidenceScore());
        result.setUnclassified(chunk.getIsUnclassified());
        result.setAmbiguous(chunk.getIsAmbiguous());
        result.setChunkIndex(chunk.getChunkIndex());
        return result;
    }
}

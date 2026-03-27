package com.incident.classifier.controller;

import com.incident.classifier.dto.Dto;
import com.incident.classifier.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Upload and retrieve classified documents")
public class DocumentController {

    private final DocumentService documentService;

    // ─── Upload PDF ───────────────────────────────────────────────────────────

    @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF document for classification",
               description = "Accepts a PDF file. Text is extracted and each chunk is classified against stored topics.")
    public ResponseEntity<Dto.DocumentResponse> uploadPdf(
            @Parameter(description = "PDF file to upload", content = @Content(mediaType = "application/pdf"))
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "paragraph") String chunkMode) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadPdf(file, chunkMode));
    }

    // ─── Upload Raw Text ──────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Submit raw text for classification",
               description = "Accepts plain text. Each paragraph or sentence is classified against stored topics.")
    public ResponseEntity<Dto.DocumentResponse> uploadText(
            @Valid @RequestBody Dto.TextDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadText(request));
    }

    // ─── Get Classification Results ───────────────────────────────────────────

    @GetMapping("/{id}/results")
    @Operation(summary = "Get classification results for a document",
               description = "Returns paginated chunk results with assigned topic and confidence score.")
    public ResponseEntity<Dto.ClassificationResultResponse> getResults(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("chunkIndex").ascending());
        return ResponseEntity.ok(documentService.getResults(id, pageable));
    }

    // ─── List All Documents ───────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all uploaded documents (paginated)")
    public ResponseEntity<Page<Dto.DocumentResponse>> listDocuments(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(documentService.listDocuments(pageable));
    }
}

# Document-to-Incident Classification System

A Spring Boot backend that accepts documents (PDF or plain text), splits them into chunks, and classifies each chunk against predefined incident topics using keyword-based scoring with fuzzy matching.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Setup Instructions](#setup-instructions)
- [API Reference](#api-reference)
- [Sample Requests & Responses](#sample-requests--responses)
- [Classification Logic](#classification-logic)
- [Database Schema](#database-schema)


---

## Architecture Overview

```
Client
  │
  ▼
Controllers  (TopicController, DocumentController, DashboardController)
  │
  ▼
Services     (TopicService, DocumentService, DashboardService)
  │
  ├──► Utils  (PdfExtractor → TextChunker → ClassificationEngine)
  │
  ▼
Repositories (JPA → MySQL)
```

**Processing pipeline for a document:**

```
Upload (PDF / Text)
        │
        ▼
  PdfExtractor (if PDF)
        │
        ▼
   TextChunker  ──► [chunk₁, chunk₂, … chunkₙ]
        │
        ▼
ClassificationEngine (per chunk)
  • Exact keyword match   → score +1.0 per occurrence
  • Partial/substring     → score +0.5
  • Levenshtein fuzzy     → score +0.3 (edit distance ≤ 1)
        │
        ▼
  ClassifiedChunk (stored with topic + confidence)
```

---

## Tech Stack

| Layer        | Technology                       |
|--------------|----------------------------------|
| Framework    | Spring Boot 3.2                  |
| Language     | Java 21                          |
| ORM          | Spring Data JPA / Hibernate      |
| Database     | MySQL 8 (H2 for tests)           |
| PDF Parsing  | Apache PDFBox 2.0.29                  |
| API Docs     | SpringDoc OpenAPI 2 (Swagger UI) |
| Build        | Maven                            |


---

## Setup Instructions

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 running locally 





```properties
spring.datasource.url=jdbc:mysql://localhost:3306/incident_classifier?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD
```

### 2. Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

The application starts on **http://localhost:8080**

### 3. Swagger UI

Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## API Reference

### Topics

| Method | Endpoint          | Description             |
|--------|-------------------|-------------------------|
| POST   | `/api/topics`     | Create a topic          |
| GET    | `/api/topics`     | List all topics         |
| GET    | `/api/topics/{id}`| Get topic by ID         |
| PUT    | `/api/topics/{id}`| Update topic            |
| DELETE | `/api/topics/{id}`| Delete topic            |

### Documents

| Method | Endpoint                          | Description                       |
|--------|-----------------------------------|-----------------------------------|
| POST   | `/api/documents`                  | Submit raw text for classification|
| POST   | `/api/documents/upload-pdf`       | Upload PDF for classification     |
| GET    | `/api/documents`                  | List all documents (paginated)    |
| GET    | `/api/documents/{id}/results`     | Get classification results        |

Query params for results: `page` (default 0), `size` (default 50)

### Dashboard

| Method | Endpoint         | Description                  |
|--------|------------------|------------------------------|
| GET    | `/api/dashboard` | Global classification stats  |

---

## Sample Requests & Responses

### 1. Create Topics

**POST** `/api/topics`

```json
{
  "title": "Delhi Bomb Blast",
  "keywords": ["Delhi", "blast", "explosion", "bomb"]
}
```

**Response 201:**
```json
{
  "id": 1,
  "title": "Delhi Bomb Blast",
  "keywords": ["delhi", "blast", "explosion", "bomb"]
}
```

---

### 2. Submit Text Document

**POST** `/api/documents`

```json
{
  "text": "A massive explosion occurred in Delhi near Connaught Place causing panic.\n\nMeanwhile in Kanpur, a similar blast was reported in an industrial area.\n\nPolice have started investigation in both cities.",
  "chunkMode": "paragraph"
}
```

**Response 201:**
```json
{
  "id": 1,
  "fileName": "text-input",
  "sourceType": "TEXT",
  "status": "COMPLETED",
  "createdAt": "2024-03-20T10:00:00",
  "totalChunks": 3
}
```

---

### 3. Get Classification Results

**GET** `/api/documents/1/results`

**Response 200:**
```json
{
  "documentId": 1,
  "status": "COMPLETED",
  "totalChunks": 3,
  "results": [
    {
      "id": 1,
      "text": "A massive explosion occurred in Delhi near Connaught Place causing panic.",
      "assignedTopic": "Delhi Bomb Blast",
      "confidence": 0.5,
      "unclassified": false,
      "ambiguous": false,
      "chunkIndex": 0
    },
    {
      "id": 2,
      "text": "Meanwhile in Kanpur, a similar blast was reported in an industrial area.",
      "assignedTopic": "Kanpur Bomb Blast",
      "confidence": 0.5,
      "unclassified": false,
      "ambiguous": false,
      "chunkIndex": 1
    },
    {
      "id": 3,
      "text": "Police have started investigation in both cities.",
      "assignedTopic": "UNCLASSIFIED",
      "confidence": 0.0,
      "unclassified": true,
      "ambiguous": false,
      "chunkIndex": 2
    }
  ]
}
```

---

### 4. Dashboard

**GET** `/api/dashboard`

**Response 200:**
```json
{
  "totalDocuments": 10,
  "totalChunks": 200,
  "topicDistribution": {
    "Delhi Bomb Blast": 90,
    "Kanpur Bomb Blast": 70,
    "UNCLASSIFIED": 40
  }
}
```

---

## Classification Logic

### Scoring Algorithm

For every `(chunk, topic)` pair, keywords are matched against the normalised chunk text:

| Match Type             | Score Weight  | Description                                 |
|------------------------|---------------|---------------------------------------------|
| Exact / full-word match| +1.0 × count  | Keyword appears verbatim in the chunk       |
| Partial / substring    | +0.5          | Keyword is substring of a token or vice versa|
| Levenshtein fuzzy      | +0.3          | Edit distance ≤ 1 (handles single typos)    |

```
confidence = topicScore / totalKeywords   (clamped to [0, 1])
```

**Decision rules:**
- **Highest score wins** → assigned topic
- **All scores = 0** → `UNCLASSIFIED`
- **Multiple topics tie** → first alphabetically wins; `ambiguous = true` flag is set

### How to improve accuracy

1. **TF-IDF weighting** — rare keywords score higher than common ones
2. **NLP tokenisation** — use a stemmer/lemmatiser (e.g. OpenNLP) to handle "blasting" → "blast"
3. **Synonym expansion** — map "explosion" ↔ "detonation" ↔ "blast" via WordNet
4. **ML model** — train a text classifier (e.g. fine-tuned BERT) on labelled incident data
5. **Named Entity Recognition** — detect city names automatically

---

## Database Schema

```sql
-- Topics
CREATE TABLE topics (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(255) NOT NULL UNIQUE,
    created_at DATETIME
);

-- Topic Keywords (1:N)
CREATE TABLE topic_keywords (
    topic_id BIGINT NOT NULL,
    keyword  VARCHAR(255),
    FOREIGN KEY (topic_id) REFERENCES topics(id)
);

-- Documents
CREATE TABLE documents (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name     VARCHAR(255),
    source_type   ENUM('PDF','TEXT'),
    original_text LONGTEXT,
    status        ENUM('PENDING','PROCESSING','COMPLETED','FAILED'),
    created_at    DATETIME
);

-- Classified Chunks
CREATE TABLE classified_chunks (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id       BIGINT NOT NULL,
    text_chunk        TEXT NOT NULL,
    assigned_topic_id BIGINT,
    confidence_score  DOUBLE,
    is_unclassified   BOOLEAN DEFAULT FALSE,
    is_ambiguous      BOOLEAN DEFAULT FALSE,
    chunk_index       INT,
    FOREIGN KEY (document_id)       REFERENCES documents(id),
    FOREIGN KEY (assigned_topic_id) REFERENCES topics(id)
);
```



---

## Scaling Considerations

| Concern             | Solution                                                        |
|---------------------|-----------------------------------------------------------------|
| Large documents     | Stream chunks; process in batches; async via `@Async`           |
| High concurrency    | Add Kafka queue; workers consume and classify asynchronously    |
| Classification speed| Cache topic keywords in memory; avoid DB calls per chunk        |
| AI integration      | Swap `ClassificationEngine` for an LLM/ML microservice endpoint |

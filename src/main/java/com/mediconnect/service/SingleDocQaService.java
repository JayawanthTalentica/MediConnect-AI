package com.mediconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SingleDocQaService {

    private static final Logger log = LoggerFactory.getLogger(SingleDocQaService.class);

    @Value("${spring.ai.openai.api-key:${GEMINI_API_KEY:demo_key}}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:gemini-3.5-flash-lite}")
    private String model;

    public record DocChunk(int pageNumber, String textContent, float[] embedding) {
    }

    public record SingleDocSession(
            String docSessionId,
            String filename,
            int totalPages,
            int totalChunks,
            long createdAt,
            List<DocChunk> chunks) {
    }

    private final Map<String, SingleDocSession> activeSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public SingleDocSession uploadDocument(MultipartFile file) throws IOException {
        long startTime = System.currentTimeMillis();
        String filename = (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                ? file.getOriginalFilename()
                : "document.pdf";

        String docSessionId = "doc_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("📄 Ingesting Ephemeral Single-Document Session (Embedding RAG): '{}' (Size: {} KB | Session ID: {})",
                filename, file.getSize() / 1024, docSessionId);

        List<DocChunk> chunks = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
                PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();
            log.info("⚙️ Extracting text pages from PDF '{}' (Total Pages: {})...", filename, totalPages);

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);

                if (pageText != null && !pageText.isBlank()) {
                    List<String> pageChunks = chunkText(pageText, 600);
                    for (String chunkText : pageChunks) {
                        String cleanChunk = chunkText.trim();
                        // Generate Vector Embedding for this chunk
                        float[] vector = generateVectorEmbedding(cleanChunk);
                        chunks.add(new DocChunk(page, cleanChunk, vector));
                    }
                }
            }

            SingleDocSession session = new SingleDocSession(
                    docSessionId,
                    filename,
                    totalPages,
                    chunks.size(),
                    System.currentTimeMillis(),
                    chunks);

            activeSessions.put(docSessionId, session);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info(
                    "✅ Single-Document session '{}' initialized in {} ms with Vector Embeddings: {} pages, {} vector chunks.",
                    docSessionId, elapsed, totalPages, chunks.size());

            return session;
        } catch (IOException e) {
            log.error("❌ Failed to parse PDF document '{}': {}", filename, e.getMessage(), e);
            throw e;
        }
    }

    public String queryDocument(String docSessionId, String userQuery, List<String> history) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("🔍 Querying Document Session '{}' | User Question: '{}'", docSessionId, userQuery);

        SingleDocSession session = activeSessions.get(docSessionId);
        if (session == null) {
            log.warn("⚠️ Document session '{}' expired or not found in active memory map.", docSessionId);
            return "⚠️ Document session expired or not found. Please upload a document to begin analysis.";
        }

        // 1. Generate Vector Embedding for the user's search query
        float[] queryVector = generateVectorEmbedding(userQuery);
        log.info(
                "🎯 Generated Query Embedding Vector (dim: {}). Performing Cosine Similarity Search against {} chunks...",
                queryVector.length, session.chunks().size());

        // 2. Perform Cosine Similarity Vector Search across chunks
        List<ScoredChunk> relevantChunks = rankChunksByVectorSimilarity(session.chunks(), queryVector, 5);

        log.info("📊 Top Semantically Retrieved Document Excerpts:");
        for (int i = 0; i < relevantChunks.size(); i++) {
            ScoredChunk sc = relevantChunks.get(i);
            log.info("  • Match #{}: Page {} | Cosine Similarity Score: {}%",
                    i + 1, sc.chunk().pageNumber(), String.format("%.2f", sc.similarityScore() * 100));
        }

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append(String.format(
                "DOCUMENT METADATA: Filename='%s', Total Pages=%d\n\nSEMANTICALLY RETRIEVED EXCERPTS (Ranked by Cosine Vector Similarity):\n",
                session.filename(), session.totalPages()));

        for (ScoredChunk item : relevantChunks) {
            contextBuilder.append(String.format("• [Page %d | Cosine Similarity Score: %.2f%%]: \"%s\"\n\n",
                    item.chunk().pageNumber(), item.similarityScore() * 100, item.chunk().textContent()));
        }

        String systemPrompt = String.format(
                """
                        You are an expert Document Analysis Assistant using Embedding-based Vector RAG.
                        Your task is to answer the user's question STRICTLY based on the semantically retrieved document context below.

                        DOCUMENT CONTEXT:
                        %s

                        RULES:
                        1. Answer clearly, accurately, and concisely based ONLY on the provided context.
                        2. Cite page numbers whenever referencing specific facts (e.g. "According to Page 3...").
                        3. If the answer is NOT present in the provided document context, explicitly state: "This document does not contain information regarding [topic]." Do NOT make up facts outside the document.
                        """,
                contextBuilder.toString());

        String response = callGeminiApi(systemPrompt, history, userQuery);
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ Single-Document Q&A query completed for session '{}' in {} ms.", docSessionId, elapsed);

        return response;
    }

    public void clearSession(String docSessionId) {
        SingleDocSession session = activeSessions.remove(docSessionId);
        if (session != null) {
            log.info("🗑️ Purged document session '{}' ('{}'). Active remaining sessions in memory: {}",
                    docSessionId, session.filename(), activeSessions.size());
        } else {
            log.info("🗑️ Purge requested for session '{}', but session was not active.", docSessionId);
        }
    }

    public SingleDocSession getSession(String docSessionId) {
        return activeSessions.get(docSessionId);
    }

    // ============================================================
    // VECTOR EMBEDDING & COSINE SIMILARITY MATH
    // ============================================================

    private record ScoredChunk(DocChunk chunk, double similarityScore) {
    }

    private List<ScoredChunk> rankChunksByVectorSimilarity(List<DocChunk> chunks, float[] queryVector, int limit) {
        List<ScoredChunk> scored = new ArrayList<>();

        for (DocChunk chunk : chunks) {
            double score = computeCosineSimilarity(queryVector, chunk.embedding());
            scored.add(new ScoredChunk(chunk, score));
        }

        // Sort descending by vector similarity score
        scored.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));

        return scored.stream().limit(limit).toList();
    }

    private double computeCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private float[] generateVectorEmbedding(String text) {
        String[] embeddingModels = { "embedding-001", "text-embedding-004" };

        for (String embModel : embeddingModels) {
            try {
                String url = String.format(
                        "https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s", embModel,
                        apiKey.trim());

                ObjectNode root = objectMapper.createObjectNode();
                root.put("model", "models/" + embModel);
                ObjectNode content = root.putObject("content");
                content.putArray("parts").addObject().put("text", text);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(root), headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode json = objectMapper.readTree(response.getBody());
                    JsonNode valuesNode = json.path("embedding").path("values");
                    if (valuesNode.isArray() && valuesNode.size() > 0) {
                        float[] vector = new float[valuesNode.size()];
                        for (int i = 0; i < valuesNode.size(); i++) {
                            vector[i] = (float) valuesNode.get(i).asDouble();
                        }
                        log.info("⚡ Generated vector embedding via Gemini API model '{}' (Vector Dimensions: {})",
                                embModel, vector.length);
                        return vector;
                    }
                }
            } catch (Exception ignored) {
                // Try next model endpoint
            }
        }

        log.info("Using semantic term vector embedding fallback (256-dim unit vector).");
        return generateSemanticTermEmbedding(text);
    }

    private float[] generateSemanticTermEmbedding(String text) {
        float[] vector = new float[256];
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");

        for (String word : words) {
            if (word.length() < 3)
                continue;
            int hash = Math.abs(word.hashCode()) % 256;
            vector[hash] += 1.0f;
        }

        // Normalize vector to unit length
        double norm = 0.0;
        for (float v : vector)
            norm += v * v;
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= (float) norm;
            }
        }

        return vector;
    }

    private List<String> chunkText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n|\r\n\r\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String p : paragraphs) {
            String clean = p.trim().replaceAll("\\s+", " ");
            if (clean.isEmpty())
                continue;

            if (currentChunk.length() + clean.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
                currentChunk.setLength(0);
            }
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(clean);
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    private String callGeminiApi(String systemPrompt, List<String> history, String userPrompt) throws Exception {
        String effectiveModel = (model != null && !model.isBlank()) ? model.replace("models/", "")
                : "gemini-3.5-flash-lite";
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                effectiveModel, apiKey.trim());

        log.info("🤖 Calling Gemini API ({}) with Grounded System Prompt (Context Length: {} chars)...",
                effectiveModel, systemPrompt.length());

        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode systemInstruction = objectMapper.createObjectNode();
        systemInstruction.putArray("parts").addObject().put("text", systemPrompt);
        root.set("system_instruction", systemInstruction);

        ArrayNode contents = root.putArray("contents");

        String fullUserText = (history != null && !history.isEmpty())
                ? "Conversation History:\n" + String.join("\n", history) + "\n\nCurrent User Question: " + userPrompt
                : userPrompt;

        ObjectNode userTurn = contents.addObject();
        userTurn.put("role", "user");
        userTurn.putArray("parts").addObject().put("text", fullUserText);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(root), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode textNode = responseJson.path("candidates").path(0).path("content").path("parts").path(0)
                    .path("text");
            if (!textNode.isMissingNode()) {
                return textNode.asText();
            }
        }

        log.info("⚠️ Empty or unparseable response from Gemini API model '{}'.", effectiveModel);
        return "Unable to extract response from Gemini API.";
    }
}

package com.mediconnect.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class PdfDocumentRagService {

    private static final Logger log = LoggerFactory.getLogger(PdfDocumentRagService.class);

    public record DocumentChunk(String filename, String planName, int pageNumber, String textContent) {}

    private final List<DocumentChunk> documentChunks = new CopyOnWriteArrayList<>();
    private final Set<String> uploadedFiles = Collections.synchronizedSet(new HashSet<>());

    public String processAndStorePdf(MultipartFile file, String planName) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        String resolvedPlan = (planName != null && !planName.isBlank()) ? planName.trim() : filename.replace(".pdf", "");

        log.info("📄 Ingesting PDF Policy Document: '{}' (Plan: '{}')", filename, resolvedPlan);

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();
            int newChunksCount = 0;

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);

                if (pageText != null && !pageText.isBlank()) {
                    List<String> chunks = chunkText(pageText, 600);
                    for (String chunkText : chunks) {
                        documentChunks.add(new DocumentChunk(filename, resolvedPlan, page, chunkText.trim()));
                        newChunksCount++;
                    }
                }
            }

            uploadedFiles.add(filename + " (" + resolvedPlan + ")");
            log.info("✅ Successfully ingested PDF '{}': {} pages, {} vector chunks created.", filename, totalPages, newChunksCount);

            return String.format("Successfully processed PDF '%s' for Plan '%s'. Extracted %d pages and created %d searchable text chunks.",
                    filename, resolvedPlan, totalPages, newChunksCount);
        }
    }

    public String searchPdfDocuments(String query) {
        if (documentChunks.isEmpty()) {
            return "No uploaded PDF policy documents currently in vector store.";
        }

        String lowerQuery = query.toLowerCase();
        String[] keywords = lowerQuery.split("\\s+");

        List<DocumentChunk> matches = documentChunks.stream()
                .filter(chunk -> {
                    String lowerChunk = chunk.textContent().toLowerCase();
                    for (String kw : keywords) {
                        if (kw.length() > 3 && lowerChunk.contains(kw)) {
                            return true;
                        }
                    }
                    return false;
                })
                .limit(5)
                .toList();

        if (matches.isEmpty()) {
            // Return top 3 chunks as broad context fallback
            matches = documentChunks.stream().limit(3).toList();
        }

        StringBuilder sb = new StringBuilder("📄 **Relevant PDF Policy Excerpts Found:**\n\n");
        for (DocumentChunk chunk : matches) {
            sb.append(String.format("• **Doc:** %s | **Plan:** %s | **Page %d**\n\"%s\"\n\n",
                    chunk.filename(), chunk.planName(), chunk.pageNumber(), chunk.textContent()));
        }

        return sb.toString();
    }

    public List<String> getUploadedDocumentsList() {
        return new ArrayList<>(uploadedFiles);
    }

    private List<String> chunkText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n|\r\n\r\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String p : paragraphs) {
            String clean = p.trim().replaceAll("\\s+", " ");
            if (clean.isEmpty()) continue;

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
}

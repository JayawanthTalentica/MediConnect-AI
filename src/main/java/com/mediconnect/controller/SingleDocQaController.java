package com.mediconnect.controller;

import com.mediconnect.service.SingleDocQaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doc-qa")
@CrossOrigin(origins = "*")
public class SingleDocQaController {

    private final SingleDocQaService docQaService;

    public SingleDocQaController(SingleDocQaService docQaService) {
        this.docQaService = docQaService;
    }

    public record DocQueryRequest(String docSessionId, String message, List<String> history) {}

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Please upload a valid PDF file (.pdf)"
            ));
        }

        try {
            var session = docQaService.uploadDocument(file);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "docSessionId", session.docSessionId(),
                    "filename", session.filename(),
                    "totalPages", session.totalPages(),
                    "totalChunks", session.totalChunks(),
                    "message", String.format("Successfully parsed '%s'. Ready for single-document Q&A across %d pages.", session.filename(), session.totalPages())
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to process PDF document: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/query")
    public ResponseEntity<?> queryDocument(@RequestBody DocQueryRequest request) {
        if (request.docSessionId() == null || request.docSessionId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Document session ID is required."
            ));
        }

        try {
            String answer = docQaService.queryDocument(request.docSessionId(), request.message(), request.history());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "docSessionId", request.docSessionId(),
                    "response", answer
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Error querying document: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/session/{docSessionId}")
    public ResponseEntity<?> clearSession(@PathVariable String docSessionId) {
        docQaService.clearSession(docSessionId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Document session " + docSessionId + " cleared."
        ));
    }

    @GetMapping("/session/{docSessionId}")
    public ResponseEntity<?> getSessionInfo(@PathVariable String docSessionId) {
        var session = docQaService.getSession(docSessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }
}

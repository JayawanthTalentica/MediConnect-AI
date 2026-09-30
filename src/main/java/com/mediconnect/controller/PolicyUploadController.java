package com.mediconnect.controller;

import com.mediconnect.service.PdfDocumentRagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policy")
@CrossOrigin(origins = "*")
public class PolicyUploadController {

    private final PdfDocumentRagService pdfDocumentRagService;

    public PolicyUploadController(PdfDocumentRagService pdfDocumentRagService) {
        this.pdfDocumentRagService = pdfDocumentRagService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadPolicyPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "planName", required = false) String planName) {

        if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Please upload a valid PDF file (.pdf)"));
        }

        try {
            String result = pdfDocumentRagService.processAndStorePdf(file, planName);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "filename", file.getOriginalFilename(),
                    "message", result
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to process PDF: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<String>> listUploadedDocuments() {
        return ResponseEntity.ok(pdfDocumentRagService.getUploadedDocumentsList());
    }
}

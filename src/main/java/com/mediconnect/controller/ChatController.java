package com.mediconnect.controller;

import com.mediconnect.agent.MediConnectAgentService;
import com.mediconnect.service.ChatMemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final MediConnectAgentService agentService;
    private final ChatMemoryService chatMemoryService;

    public ChatController(MediConnectAgentService agentService, ChatMemoryService chatMemoryService) {
        this.agentService = agentService;
        this.chatMemoryService = chatMemoryService;
    }

    public record ChatRequest(String sessionId, String message) {}
    public record ChatResponse(String sessionId, String response, List<String> history) {}

    @PostMapping
    public ResponseEntity<ChatResponse> processChat(@RequestBody ChatRequest request) {
        String sessionId = (request.sessionId() != null && !request.sessionId().isBlank()) 
                ? request.sessionId() 
                : UUID.randomUUID().toString().substring(0, 8);

        String agentResponse = agentService.processUserChat(sessionId, request.message());
        List<String> history = chatMemoryService.getHistory(sessionId);

        return ResponseEntity.ok(new ChatResponse(sessionId, agentResponse, history));
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<String>> getChatHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatMemoryService.getHistory(sessionId));
    }

    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, String>> clearHistory(@PathVariable String sessionId) {
        chatMemoryService.clearHistory(sessionId);
        return ResponseEntity.ok(Map.of("status", "cleared", "sessionId", sessionId));
    }
}

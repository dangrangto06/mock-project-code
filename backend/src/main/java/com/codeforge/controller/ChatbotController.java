package com.codeforge.controller;

import com.codeforge.dto.ChatbotDto.ChatHistoryResponse;
import com.codeforge.dto.ChatbotDto.ChatRequest;
import com.codeforge.dto.ChatbotDto.ChatResponse;
import com.codeforge.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for CodeForge AI Chatbot (ForgeBot) interactions.
 * Synchronized with backend database history and user sessions.
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Endpoint to receive user inquiries, synchronize message history, and return domain-guarded English responses.
     */
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> askQuestion(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        ChatResponse response = chatbotService.processChat(request, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves synchronized conversation history for a conversation or user.
     */
    @GetMapping("/history")
    public ResponseEntity<ChatHistoryResponse> getHistory(
            @RequestParam(required = false) String conversationId,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        ChatHistoryResponse history = chatbotService.getHistory(conversationId, username);
        return ResponseEntity.ok(history);
    }

    /**
     * Clears synchronized chat history for a conversation or user.
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearHistory(
            @RequestParam(required = false) String conversationId,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        chatbotService.clearHistory(conversationId, username);
        return ResponseEntity.ok(Map.of("message", "Chat history cleared successfully."));
    }
}

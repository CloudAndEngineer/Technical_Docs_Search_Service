package org.example.chat.controller;

import jakarta.validation.Valid;
import org.example.chat.dto.request.ChatRequest;
import org.example.chat.service.ChatService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/chat")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping
	public void chat(@PathVariable UUID sessionId, @Valid @RequestBody ChatRequest request) {
		chatService.answer(sessionId, request);
	}
}

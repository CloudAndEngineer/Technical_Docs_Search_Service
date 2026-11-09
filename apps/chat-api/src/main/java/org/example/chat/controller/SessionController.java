package org.example.chat.controller;

import org.example.chat.dto.response.SessionResponse;
import org.example.chat.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SessionController {

	private final SessionService sessions;

	public SessionController(SessionService sessions) {
		this.sessions = sessions;
	}

	@PostMapping("/notebooks/{notebookId}/sessions")
	@ResponseStatus(HttpStatus.CREATED)
	public SessionResponse create(@PathVariable UUID notebookId) {
		return sessions.create(notebookId);
	}

	@GetMapping("/notebooks/{notebookId}/sessions")
	public List<SessionResponse> listByNotebook(@PathVariable UUID notebookId) {
		return sessions.listByNotebook(notebookId);
	}

	@GetMapping("/sessions/{sessionId}")
	public SessionResponse get(@PathVariable UUID sessionId) {
		return sessions.get(sessionId);
	}
}

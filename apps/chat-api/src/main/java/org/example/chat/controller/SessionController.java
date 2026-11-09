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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

	private final SessionService sessions;

	public SessionController(SessionService sessions) {
		this.sessions = sessions;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SessionResponse create() {
		return sessions.create();
	}

	@GetMapping("/{sessionId}")
	public SessionResponse get(@PathVariable UUID sessionId) {
		return sessions.get(sessionId);
	}
}

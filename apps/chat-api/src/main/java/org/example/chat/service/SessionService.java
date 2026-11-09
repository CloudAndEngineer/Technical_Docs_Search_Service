package org.example.chat.service;

import org.example.chat.domain.ChatSession;
import org.example.chat.dto.response.SessionResponse;
import org.example.chat.exception.ApiException;
import org.example.chat.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SessionService {

	private final ChatSessionRepository sessions;

	public SessionService(ChatSessionRepository sessions) {
		this.sessions = sessions;
	}

	@Transactional
	public SessionResponse create() {
		ChatSession session = sessions.save(new ChatSession(UUID.randomUUID()));
		return new SessionResponse(session.getId(), session.getCreatedAt());
	}

	@Transactional(readOnly = true)
	public SessionResponse get(UUID id) {
		ChatSession session = sessions.findById(id)
				.orElseThrow(() -> new ApiException("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다."));
		return new SessionResponse(session.getId(), session.getCreatedAt());
	}
}

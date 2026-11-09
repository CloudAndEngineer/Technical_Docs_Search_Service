package org.example.chat.service;

import org.example.chat.domain.ChatSession;
import org.example.chat.dto.response.SessionResponse;
import org.example.chat.exception.ApiException;
import org.example.chat.repository.NotebookRepository;
import org.example.chat.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
public class SessionService {

	private final ChatSessionRepository sessions;
	private final NotebookRepository notebooks;

	public SessionService(ChatSessionRepository sessions, NotebookRepository notebooks) {
		this.sessions = sessions;
		this.notebooks = notebooks;
	}

	@Transactional
	public SessionResponse create(UUID notebookId) {
		var notebook = notebooks.findById(notebookId)
				.orElseThrow(() -> new ApiException("NOTEBOOK_NOT_FOUND", "노트북을 찾을 수 없습니다."));
		ChatSession session = sessions.save(new ChatSession(UUID.randomUUID(), notebook));
		return toResponse(session);
	}

	@Transactional(readOnly = true)
	public SessionResponse get(UUID id) {
		ChatSession session = sessions.findById(id)
				.orElseThrow(() -> new ApiException("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다."));
		return toResponse(session);
	}

	@Transactional(readOnly = true)
	public List<SessionResponse> listByNotebook(UUID notebookId) {
		if (!notebooks.existsById(notebookId)) {
			throw new ApiException("NOTEBOOK_NOT_FOUND", "노트북을 찾을 수 없습니다.");
		}
		return sessions.findAllByNotebook_IdOrderByCreatedAtDesc(notebookId).stream()
				.map(this::toResponse).toList();
	}

	private SessionResponse toResponse(ChatSession session) {
		return new SessionResponse(session.getId(), session.getNotebookId(), session.getCreatedAt());
	}
}

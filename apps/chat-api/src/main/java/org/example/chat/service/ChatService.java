package org.example.chat.service;

import org.example.chat.dto.request.ChatRequest;
import org.example.chat.exception.ApiException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChatService {

	private final SessionService sessions;

	public ChatService(SessionService sessions) {
		this.sessions = sessions;
	}

	public void answer(UUID sessionId, ChatRequest request) {
		sessions.get(sessionId);
		throw new ApiException("RAG_NOT_CONFIGURED", "문서 검색 및 AI 답변 기능이 아직 연결되지 않았습니다.");
	}
}

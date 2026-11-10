package org.example.chat.service;

import org.example.chat.dto.request.ChatRequest;
import org.example.chat.dto.response.ChatResponse;
import org.example.chat.dto.response.ChatSourceResponse;
import org.example.chat.exception.ApiException;
import org.example.chat.repository.DocumentChunkSearchRepository;
import org.example.chat.repository.RetrievedChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class ChatService {

	private static final String NO_RELEVANT_DOCUMENTS = "등록된 문서 중 연관된 기술 정보를 찾을 수 없습니다.";

	private final SessionService sessions;
	private final GeminiService gemini;
	private final DocumentChunkSearchRepository chunks;
	private final int retrievalLimit;

	public ChatService(SessionService sessions, GeminiService gemini,
			DocumentChunkSearchRepository chunks,
			@Value("${gemini.retrieval-limit}") int retrievalLimit) {
		this.sessions = sessions;
		this.gemini = gemini;
		this.chunks = chunks;
		this.retrievalLimit = retrievalLimit;
	}

	public ChatResponse answer(UUID sessionId, ChatRequest request) {
		var session = sessions.get(sessionId);
		float[] questionEmbedding = gemini.embedQuestion(request.question());
		List<RetrievedChunk> retrieved = chunks.findNearest(
				session.notebookId(), questionEmbedding, retrievalLimit);

		if (retrieved.isEmpty()) {
			return new ChatResponse(NO_RELEVANT_DOCUMENTS, List.of());
		}

		String prompt = buildPrompt(request.question(), retrieved);
		String answer = gemini.generateAnswer(prompt);
		List<ChatSourceResponse> sources = IntStream.range(0, retrieved.size())
				.mapToObj(index -> toSource(index + 1, retrieved.get(index)))
				.toList();
		return new ChatResponse(answer, sources);
	}

	private String buildPrompt(String question, List<RetrievedChunk> retrieved) {
		StringBuilder prompt = new StringBuilder(
				"다음은 선택한 노트북에서 검색한 참고 문서입니다. 답변의 주요 주장 뒤에 근거 자료 번호를 [자료 1] 형식으로 표시하세요.\n");
		for (int i = 0; i < retrieved.size(); i++) {
			RetrievedChunk chunk = retrieved.get(i);
			prompt.append("\n[자료 ").append(i + 1).append("]\n");
			if (chunk.documentTitle() != null) {
				prompt.append("제목: ").append(chunk.documentTitle()).append('\n');
			}
			if (chunk.fileName() != null) {
				prompt.append("파일명: ").append(chunk.fileName()).append('\n');
			}
			if (chunk.sourceUrl() != null) {
				prompt.append("URL: ").append(chunk.sourceUrl()).append('\n');
			}
			prompt.append("내용:\n").append(chunk.content()).append('\n');
		}
		return prompt.append("\n질문: ").append(question).toString();
	}

	private ChatSourceResponse toSource(int reference, RetrievedChunk chunk) {
		return new ChatSourceResponse(reference, chunk.documentTitle(), chunk.fileName(), chunk.sourceUrl());
	}
}

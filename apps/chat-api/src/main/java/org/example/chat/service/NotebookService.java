package org.example.chat.service;

import org.example.chat.domain.Notebook;
import org.example.chat.dto.request.CreateNotebookRequest;
import org.example.chat.dto.response.NotebookResponse;
import org.example.chat.exception.ApiException;
import org.example.chat.repository.NotebookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotebookService {

	private final NotebookRepository notebooks;

	public NotebookService(NotebookRepository notebooks) {
		this.notebooks = notebooks;
	}

	@Transactional
	public NotebookResponse create(CreateNotebookRequest request) {
		Notebook notebook = notebooks.save(new Notebook(UUID.randomUUID(), request.title().trim()));
		return toResponse(notebook);
	}

	@Transactional(readOnly = true)
	public List<NotebookResponse> list() {
		return notebooks.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
	}

	@Transactional
	public void delete(UUID notebookId) {
		Notebook notebook = notebooks.findById(notebookId)
				.orElseThrow(() -> new ApiException("NOTEBOOK_NOT_FOUND", "노트북을 찾을 수 없습니다."));
		notebooks.delete(notebook);
	}

	@Transactional(readOnly = true)
	public Notebook getEntity(UUID notebookId) {
		return notebooks.findById(notebookId)
				.orElseThrow(() -> new ApiException("NOTEBOOK_NOT_FOUND", "노트북을 찾을 수 없습니다."));
	}

	private NotebookResponse toResponse(Notebook notebook) {
		return new NotebookResponse(notebook.getId(), notebook.getTitle(), notebook.getCreatedAt());
	}
}

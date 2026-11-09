package org.example.chat.controller;

import jakarta.validation.Valid;
import org.example.chat.dto.request.CreateNotebookRequest;
import org.example.chat.dto.response.NotebookResponse;
import org.example.chat.service.NotebookService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notebooks")
public class NotebookController {

	private final NotebookService notebooks;

	public NotebookController(NotebookService notebooks) {
		this.notebooks = notebooks;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public NotebookResponse create(@Valid @RequestBody CreateNotebookRequest request) {
		return notebooks.create(request);
	}

	@GetMapping
	public List<NotebookResponse> list() {
		return notebooks.list();
	}

	@DeleteMapping("/{notebookId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID notebookId) {
		notebooks.delete(notebookId);
	}
}

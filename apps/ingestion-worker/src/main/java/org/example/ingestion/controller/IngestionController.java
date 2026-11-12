package org.example.ingestion.controller;

import org.example.ingestion.dto.IngestionJobResponse;
import org.example.ingestion.service.IngestionJobService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
public class IngestionController {

	private final IngestionJobService jobs;

	public IngestionController(IngestionJobService jobs) {
		this.jobs = jobs;
	}

	@PostMapping(path = "/api/v1/notebooks/{notebookId}/documents", consumes = "multipart/form-data")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public IngestionJobResponse upload(
			@PathVariable UUID notebookId,
			@RequestPart("file") MultipartFile file,
			@RequestPart(value = "title", required = false) String title) throws IOException {
		return jobs.enqueueUpload(notebookId, title, file.getOriginalFilename(), file.getBytes());
	}

	@GetMapping("/api/v1/ingestion-jobs/{jobId}")
	public IngestionJobResponse getStatus(@PathVariable UUID jobId) {
		return jobs.getJob(jobId);
	}
}
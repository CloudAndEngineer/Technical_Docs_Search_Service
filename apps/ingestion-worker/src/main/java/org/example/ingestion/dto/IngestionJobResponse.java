package org.example.ingestion.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IngestionJobResponse(
		UUID jobId,
		UUID notebookId,
		String type,
		String status,
		String documentTitle,
		String fileName,
		List<String> sourceUrls,
		String errorCode,
		String errorMessage,
		Instant createdAt,
		Instant updatedAt
) {
}

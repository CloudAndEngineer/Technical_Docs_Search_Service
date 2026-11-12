package org.example.ingestion.dto;

import java.time.Instant;
import java.util.UUID;

public record IngestionJobResponse(
		UUID jobId,
		UUID notebookId,
		String type,
		String status,
		String documentTitle,
		String fileName,
		String errorCode,
		String errorMessage,
		Instant createdAt,
		Instant updatedAt
) {
}

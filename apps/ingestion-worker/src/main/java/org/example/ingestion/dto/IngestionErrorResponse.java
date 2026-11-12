package org.example.ingestion.dto;

import java.time.Instant;

public record IngestionErrorResponse(Instant timestamp, int status, String code, String message, String path) {
}

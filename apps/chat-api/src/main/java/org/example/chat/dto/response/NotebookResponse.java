package org.example.chat.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NotebookResponse(UUID notebookId, String title, Instant createdAt) {
}

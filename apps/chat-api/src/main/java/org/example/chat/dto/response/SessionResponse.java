package org.example.chat.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID sessionId, UUID notebookId, Instant createdAt) {
}

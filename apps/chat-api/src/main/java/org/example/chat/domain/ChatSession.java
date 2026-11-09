package org.example.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {

	@Id
	@Column(name = "session_id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ChatSession() {
	}

	public ChatSession(UUID id) {
		this.id = id;
	}

	@PrePersist
	void setCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

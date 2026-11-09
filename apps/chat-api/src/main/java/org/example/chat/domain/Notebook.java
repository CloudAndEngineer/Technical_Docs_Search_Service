package org.example.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notebooks")
public class Notebook {

	@Id
	@Column(name = "notebook_id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "title", nullable = false, length = 120)
	private String title;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Notebook() {
	}

	public Notebook(UUID id, String title) {
		this.id = id;
		this.title = title;
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

	public String getTitle() {
		return title;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

package org.example.chat.repository;

import org.example.chat.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
}

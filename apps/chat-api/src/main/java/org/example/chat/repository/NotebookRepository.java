package org.example.chat.repository;

import org.example.chat.domain.Notebook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotebookRepository extends JpaRepository<Notebook, UUID> {
	List<Notebook> findAllByOrderByCreatedAtDesc();
}

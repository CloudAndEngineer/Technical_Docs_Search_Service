package org.example.ingestion.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class DocumentChunkRepository {

	private static final String INSERT_SQL = """
			INSERT INTO document_chunks (
				chunk_id, notebook_id, content, embedding, document_title, file_name, source_url, created_at
			) VALUES (?, ?, ?, ?::vector, ?, ?, ?, CURRENT_TIMESTAMP)
			""";

	private final JdbcTemplate jdbcTemplate;

	public DocumentChunkRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public UUID save(UUID notebookId, String content, float[] embedding,
			String documentTitle, String fileName, String sourceUrl) {
		UUID chunkId = UUID.randomUUID();
		jdbcTemplate.update(INSERT_SQL,
				chunkId,
				notebookId,
				content,
				toPgVectorLiteral(embedding),
				documentTitle,
				fileName,
				sourceUrl);
		return chunkId;
	}

	private String toPgVectorLiteral(float[] values) {
		if (values == null || values.length == 0) {
			throw new IllegalArgumentException("Embedding vector must contain at least one value.");
		}

		StringBuilder literal = new StringBuilder("[");
		for (int i = 0; i < values.length; i++) {
			if (!Float.isFinite(values[i])) {
				throw new IllegalArgumentException("Embedding vector values must be finite.");
			}
			if (i > 0) {
				literal.append(',');
			}
			literal.append(Float.toString(values[i]));
		}
		return literal.append(']').toString();
	}
}

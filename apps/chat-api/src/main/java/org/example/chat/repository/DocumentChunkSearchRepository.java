package org.example.chat.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class DocumentChunkSearchRepository {

	private static final String SEARCH_SQL = """
			SELECT content, document_title, file_name, source_url,
			       1 - (embedding <=> ?::vector) AS similarity
			FROM document_chunks
			WHERE notebook_id = ?
			ORDER BY embedding <=> ?::vector
			LIMIT ?
			""";

	private final JdbcTemplate jdbcTemplate;

	public DocumentChunkSearchRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<RetrievedChunk> findNearest(UUID notebookId, float[] embedding, int limit) {
		String vectorLiteral = toPgVectorLiteral(embedding);
		return jdbcTemplate.query(SEARCH_SQL, (resultSet, rowNumber) -> new RetrievedChunk(
				resultSet.getString("content"),
				resultSet.getString("document_title"),
				resultSet.getString("file_name"),
				resultSet.getString("source_url"),
				resultSet.getDouble("similarity")),
			vectorLiteral, notebookId, vectorLiteral, limit);
	}

	private String toPgVectorLiteral(float[] values) {
		if (values == null || values.length == 0) {
			throw new IllegalArgumentException("Embedding vector must not be empty.");
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

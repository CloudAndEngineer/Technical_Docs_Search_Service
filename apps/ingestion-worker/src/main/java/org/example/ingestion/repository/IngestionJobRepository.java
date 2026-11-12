package org.example.ingestion.repository;

import org.example.ingestion.dto.IngestionJobResponse;
import org.example.ingestion.exception.IngestionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class IngestionJobRepository {

	private final JdbcTemplate jdbcTemplate;

	public IngestionJobRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean notebookExists(UUID notebookId) {
		Boolean exists = jdbcTemplate.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM notebooks WHERE notebook_id = ?)", Boolean.class, notebookId);
		return Boolean.TRUE.equals(exists);
	}

	public IngestionJobResponse create(UUID jobId, UUID notebookId, String type, String documentTitle, String fileName) {
		jdbcTemplate.update("""
				INSERT INTO ingestion_jobs (job_id, notebook_id, job_type, status, document_title, file_name)
				VALUES (?, ?, ?, 'QUEUED', ?, ?)
				""", jobId, notebookId, type, documentTitle, fileName);
		return get(jobId);
	}

	public IngestionJobResponse get(UUID jobId) {
		return jdbcTemplate.query("""
				SELECT job_id, notebook_id, job_type, status, document_title, file_name,
			       error_code, error_message, created_at, updated_at
			FROM ingestion_jobs WHERE job_id = ?
			""", resultSet -> {
			if (!resultSet.next()) {
				throw new IngestionException("JOB_NOT_FOUND", "처리 작업을 찾을 수 없습니다.");
			}
			return toResponse(resultSet);
		}, jobId);
	}

	public void updateStatus(UUID jobId, String status, String errorCode, String errorMessage) {
		jdbcTemplate.update("""
				UPDATE ingestion_jobs
				SET status = ?, error_code = ?, error_message = ?, updated_at = CURRENT_TIMESTAMP
				WHERE job_id = ?
				""", status, errorCode, errorMessage, jobId);
	}

	public void failUnfinishedJobsAfterRestart() {
		jdbcTemplate.update("""
				UPDATE ingestion_jobs
				SET status = 'FAILED', error_code = 'WORKER_RESTARTED',
				    error_message = 'Worker가 재시작되어 작업이 중단되었습니다.', updated_at = CURRENT_TIMESTAMP
				WHERE status IN ('QUEUED', 'PROCESSING')
				""");
	}

	private IngestionJobResponse toResponse(ResultSet resultSet) throws SQLException {
		return new IngestionJobResponse(
				resultSet.getObject("job_id", UUID.class),
				resultSet.getObject("notebook_id", UUID.class),
				resultSet.getString("job_type"),
				resultSet.getString("status"),
				resultSet.getString("document_title"),
				resultSet.getString("file_name"),
				resultSet.getString("error_code"),
				resultSet.getString("error_message"),
				instant(resultSet.getTimestamp("created_at")),
				instant(resultSet.getTimestamp("updated_at")));
	}

	private Instant instant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}
}

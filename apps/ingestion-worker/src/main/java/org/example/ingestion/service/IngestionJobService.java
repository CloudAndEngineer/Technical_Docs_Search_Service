package org.example.ingestion.service;

import org.example.ingestion.dto.IngestionJobResponse;
import org.example.ingestion.exception.IngestionException;
import org.example.ingestion.parser.DocumentParser;
import org.example.ingestion.repository.IngestionJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class IngestionJobService {

	private final IngestionJobRepository jobs;
	private final IngestionPipelineService pipeline;
	private final DocumentParser parser;
	private final int maxFileSizeBytes;

	public IngestionJobService(
			IngestionJobRepository jobs,
			IngestionPipelineService pipeline,
			DocumentParser parser,
			@Value("${ingestion.max-file-size-bytes}") int maxFileSizeBytes) {
		this.jobs = jobs;
		this.pipeline = pipeline;
		this.parser = parser;
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public IngestionJobResponse enqueueUpload(UUID notebookId, String originalTitle, String originalFileName, byte[] bytes) {
		ensureNotebookExists(notebookId);
		if (bytes == null || bytes.length == 0) {
			throw new IngestionException("EMPTY_FILE", "업로드한 파일이 비어 있습니다.");
		}
		if (bytes.length > maxFileSizeBytes) {
			throw new IngestionException("FILE_TOO_LARGE", "업로드 파일의 크기가 허용 범위를 초과했습니다.");
		}
		String fileName = basename(originalFileName);
		parser.validateUploadName(fileName);
		String documentTitle = originalTitle == null || originalTitle.isBlank() ? fileName : originalTitle.trim();
		if (documentTitle.length() > 500) {
			throw new IngestionException("INVALID_DOCUMENT_TITLE", "문서 제목은 500자 이내로 입력하세요.");
		}
		UUID jobId = UUID.randomUUID();
		IngestionJobResponse job = jobs.create(jobId, notebookId, "UPLOAD", documentTitle, fileName);
		try {
			pipeline.processUpload(jobId, notebookId, documentTitle, fileName, bytes);
		} catch (TaskRejectedException exception) {
			jobs.updateStatus(jobId, "FAILED", "WORKER_BUSY", "처리 대기열이 가득 찼습니다. 잠시 후 다시 시도하세요.");
		}
		return job;
	}

	public IngestionJobResponse enqueueSearch(UUID notebookId, String query) {
		ensureNotebookExists(notebookId);
		if (query == null || query.isBlank()) {
			throw new IngestionException("INVALID_SEARCH_QUERY", "검색할 기술 정보 이름을 입력하세요.");
		}
		if (query.length() > 300) {
			throw new IngestionException("INVALID_SEARCH_QUERY", "검색어는 300자 이내로 입력하세요.");
		}
		UUID jobId = UUID.randomUUID();
		IngestionJobResponse job = jobs.create(jobId, notebookId, "WEB_SEARCH", null, null);
		try {
			pipeline.processSearch(jobId, notebookId, query.trim());
		} catch (TaskRejectedException exception) {
			jobs.updateStatus(jobId, "FAILED", "WORKER_BUSY", "처리 대기열이 가득 찼습니다. 잠시 후 다시 시도하세요.");
		}
		return job;
	}

	public IngestionJobResponse getJob(UUID jobId) {
		return jobs.get(jobId);
	}

	private void ensureNotebookExists(UUID notebookId) {
		if (!jobs.notebookExists(notebookId)) {
			throw new IngestionException("NOTEBOOK_NOT_FOUND", "노트북을 찾을 수 없습니다.");
		}
	}

	private String basename(String originalFileName) {
		if (originalFileName == null || originalFileName.isBlank()) {
			throw new IngestionException("INVALID_FILE_NAME", "파일 이름을 확인할 수 없습니다.");
		}
		String normalized = originalFileName.replace('\\', '/');
		String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
		if (fileName.isBlank() || fileName.length() > 500) {
			throw new IngestionException("INVALID_FILE_NAME", "파일 이름은 1~500자여야 합니다.");
		}
		return fileName;
	}
}

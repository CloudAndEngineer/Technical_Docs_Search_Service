package org.example.ingestion.service;

import org.example.ingestion.chunker.FixedSizeOverlapChunker;
import org.example.ingestion.exception.IngestionException;
import org.example.ingestion.parser.DocumentParser;
import org.example.ingestion.repository.DocumentChunkRepository;
import org.example.ingestion.repository.IndexedChunk;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class IngestionPipelineService {

	private final org.example.ingestion.repository.IngestionJobRepository jobs;
	private final DocumentParser parser;
	private final FixedSizeOverlapChunker chunker;
	private final GeminiService gemini;
	private final DocumentChunkRepository chunks;

	public IngestionPipelineService(
			org.example.ingestion.repository.IngestionJobRepository jobs,
			DocumentParser parser,
			FixedSizeOverlapChunker chunker,
			GeminiService gemini,
			DocumentChunkRepository chunks) {
		this.jobs = jobs;
		this.parser = parser;
		this.chunker = chunker;
		this.gemini = gemini;
		this.chunks = chunks;
	}

	@Async("ingestionExecutor")
	public void processUpload(UUID jobId, UUID notebookId, String documentTitle, String fileName, byte[] bytes) {
		begin(jobId);
		try {
			String documentText = parser.parseUpload(fileName, bytes);
			indexDocument(notebookId, documentText, documentTitle, fileName);
			jobs.updateStatus(jobId, "COMPLETED", null, null);
		} catch (RuntimeException exception) {
			fail(jobId, exception);
		}
	}

	private void begin(UUID jobId) {
		jobs.updateStatus(jobId, "PROCESSING", null, null);
	}

	private void indexDocument(UUID notebookId, String text, String title, String fileName) {
		List<String> textChunks = chunker.chunk(text);
		if (textChunks.isEmpty()) {
			throw new IngestionException("EMPTY_DOCUMENT", "문서에서 색인할 텍스트를 추출하지 못했습니다.");
		}
		List<IndexedChunk> indexedChunks = new ArrayList<>(textChunks.size());
		for (String chunk : textChunks) {
			indexedChunks.add(new IndexedChunk(chunk, gemini.embedDocument(chunk, title)));
		}
		chunks.saveAll(notebookId, indexedChunks, title, fileName, null);
	}

	private void fail(UUID jobId, RuntimeException exception) {
		if (exception instanceof IngestionException ingestionException) {
			jobs.updateStatus(jobId, "FAILED", ingestionException.getCode(), ingestionException.getMessage());
		} else {
			jobs.updateStatus(jobId, "FAILED", "INGESTION_FAILED", "문서 처리 중 오류가 발생했습니다.");
		}
	}
}
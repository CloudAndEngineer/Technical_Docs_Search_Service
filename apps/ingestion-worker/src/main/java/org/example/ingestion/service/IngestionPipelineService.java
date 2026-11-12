package org.example.ingestion.service;

import org.example.ingestion.chunker.FixedSizeOverlapChunker;
import org.example.ingestion.exception.IngestionException;
import org.example.ingestion.parser.DocumentParser;
import org.example.ingestion.repository.DocumentChunkRepository;
import org.example.ingestion.repository.IndexedChunk;
import org.springframework.beans.factory.annotation.Value;
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
	private final WebSourceFetcher webFetcher;
	private final DocumentChunkRepository chunks;
	private final int searchResultLimit;

	public IngestionPipelineService(
			org.example.ingestion.repository.IngestionJobRepository jobs,
			DocumentParser parser,
			FixedSizeOverlapChunker chunker,
			GeminiService gemini,
			WebSourceFetcher webFetcher,
			DocumentChunkRepository chunks,
			@Value("${ingestion.search-result-limit}") int searchResultLimit) {
		this.jobs = jobs;
		this.parser = parser;
		this.chunker = chunker;
		this.gemini = gemini;
		this.webFetcher = webFetcher;
		this.chunks = chunks;
		this.searchResultLimit = searchResultLimit;
	}

	@Async("ingestionExecutor")
	public void processUpload(UUID jobId, UUID notebookId, String documentTitle, String fileName, byte[] bytes) {
		begin(jobId);
		try {
			String documentText = parser.parseUpload(fileName, bytes);
			indexDocument(notebookId, documentText, documentTitle, fileName, null);
			jobs.updateStatus(jobId, "COMPLETED", null, null);
		} catch (RuntimeException exception) {
			fail(jobId, exception);
		}
	}

	@Async("ingestionExecutor")
	public void processSearch(UUID jobId, UUID notebookId, String query) {
		begin(jobId);
		try {
			List<GeminiService.SearchSource> sources = gemini.searchOfficialDocumentation(query, searchResultLimit);
			int indexed = 0;
			IngestionException lastFailure = null;
			List<String> indexedUrls = new ArrayList<>();
			for (GeminiService.SearchSource source : sources) {
				try {
					WebSourceFetcher.FetchedSource fetched = webFetcher.fetch(source.url());
					String text = parser.parseWebDocument(fetched.bytes(), fetched.contentType(), fetched.url());
					String title = source.title().isBlank() ? fetched.title() : source.title();
					indexDocument(notebookId, text, title, null, fetched.url());
					indexedUrls.add(fetched.url());
					jobs.updateSourceUrls(jobId, indexedUrls);
					indexed++;
				} catch (IngestionException exception) {
					lastFailure = exception;
				}
			}
			if (indexed == 0) {
				if (lastFailure != null) throw lastFailure;
				throw new IngestionException("NO_SEARCH_RESULTS", "검색 결과에서 등록할 수 있는 공식 문서를 찾지 못했습니다.");
			}
			jobs.updateStatus(jobId, "COMPLETED", null, null);
		} catch (RuntimeException exception) {
			fail(jobId, exception);
		}
	}

	private void begin(UUID jobId) {
		jobs.updateStatus(jobId, "PROCESSING", null, null);
	}

	private void indexDocument(UUID notebookId, String text, String title, String fileName, String sourceUrl) {
		List<String> textChunks = chunker.chunk(text);
		if (textChunks.isEmpty()) {
			throw new IngestionException("EMPTY_DOCUMENT", "문서에서 색인할 텍스트를 추출하지 못했습니다.");
		}
		List<IndexedChunk> indexedChunks = new ArrayList<>(textChunks.size());
		for (String chunk : textChunks) {
			indexedChunks.add(new IndexedChunk(chunk, gemini.embedDocument(chunk, title)));
		}
		chunks.saveAll(notebookId, indexedChunks, title, fileName, sourceUrl);
	}

	private void fail(UUID jobId, RuntimeException exception) {
		if (exception instanceof IngestionException ingestionException) {
			jobs.updateStatus(jobId, "FAILED", ingestionException.getCode(), ingestionException.getMessage());
		} else {
			jobs.updateStatus(jobId, "FAILED", "INGESTION_FAILED", "문서 처리 중 오류가 발생했습니다.");
		}
	}
}

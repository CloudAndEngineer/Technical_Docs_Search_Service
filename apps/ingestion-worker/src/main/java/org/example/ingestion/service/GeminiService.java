package org.example.ingestion.service;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Tool;
import jakarta.annotation.PreDestroy;
import org.example.ingestion.exception.IngestionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiService {

	private final String generationModel;
	private final String embeddingModel;
	private final Client client;

	public GeminiService(
			@Value("${GOOGLE_API_KEY:}") String apiKey,
			@Value("${gemini.model}") String generationModel,
			@Value("${gemini.embedding-model}") String embeddingModel) {
		this.generationModel = generationModel;
		this.embeddingModel = embeddingModel;
		this.client = apiKey == null || apiKey.isBlank()
				? null
				: Client.builder().apiKey(apiKey).build();
	}

	public float[] embedDocument(String text, String title) {
		ensureConfigured();
		try {
			var response = client.models.embedContent(
					embeddingModel,
					text,
					EmbedContentConfig.builder().taskType("RETRIEVAL_DOCUMENT").title(title).build());
			List<Float> values = response.embeddings()
					.orElseThrow(() -> new IllegalStateException("Gemini returned no embedding."))
					.getFirst()
					.values()
					.orElseThrow(() -> new IllegalStateException("Gemini returned an empty embedding."));
			float[] result = new float[values.size()];
			for (int i = 0; i < values.size(); i++) {
				result[i] = values.get(i);
			}
			return result;
		} catch (com.google.genai.errors.ApiException exception) {
			throw translateApiException(exception);
		} catch (GenAiIOException exception) {
			throw new IngestionException("GEMINI_API_ERROR", "Gemini API에 연결할 수 없습니다. 네트워크 상태를 확인하세요.", exception);
		} catch (RuntimeException exception) {
			throw new IngestionException("GEMINI_API_ERROR", "문서 임베딩 요청에 실패했습니다.", exception);
		}
	}

	public List<SearchSource> searchOfficialDocumentation(String query, int resultLimit) {
		ensureConfigured();
		String prompt = query.trim() + " 에 관한 공식 기술 문서 찾아줘";
		var config = GenerateContentConfig.builder()
				.tools(Tool.builder().googleSearch(GoogleSearch.builder()).build())
				.build();
		try {
			var response = client.models.generateContent(generationModel, prompt, config);
			var candidates = response.candidates().orElse(List.of());
			if (candidates.isEmpty()) {
				throw new IngestionException("NO_SEARCH_RESULTS", "Gemini 검색 결과가 없습니다.");
			}
			var grounding = candidates.getFirst().groundingMetadata()
					.orElseThrow(() -> new IngestionException("NO_SEARCH_RESULTS", "Gemini 검색 결과에 출처 URL이 없습니다."));
			var chunks = grounding.groundingChunks().orElse(List.of());
			List<SearchSource> sources = new ArrayList<>();
			for (var chunk : chunks) {
				chunk.web().ifPresent(web -> {
					String url = web.uri().orElse("");
					if (!url.isBlank() && sources.stream().noneMatch(source -> source.url().equals(url))) {
						sources.add(new SearchSource(web.title().orElse("공식 기술 문서"), url));
					}
				});
				if (sources.size() >= resultLimit) {
					break;
				}
			}
			if (sources.isEmpty()) {
				throw new IngestionException("NO_SEARCH_RESULTS", "Gemini 검색 결과에서 문서 URL을 찾지 못했습니다.");
			}
			return sources;
		} catch (IngestionException exception) {
			throw exception;
		} catch (com.google.genai.errors.ApiException exception) {
			throw translateApiException(exception);
		} catch (GenAiIOException exception) {
			throw new IngestionException("GEMINI_API_ERROR", "Gemini API에 연결할 수 없습니다. 네트워크 상태를 확인하세요.", exception);
		} catch (RuntimeException exception) {
			throw new IngestionException("GEMINI_API_ERROR", "공식 문서 검색 요청에 실패했습니다.", exception);
		}
	}

	private IngestionException translateApiException(com.google.genai.errors.ApiException exception) {
		String message = switch (exception.code()) {
			case 401, 403 -> "Gemini API 인증에 실패했습니다. API 키와 권한을 확인하세요.";
			case 429 -> "Gemini API 요청 한도를 초과했습니다.";
			case 400 -> "Gemini API가 요청을 거부했습니다. 모델 및 요청 설정을 확인하세요.";
			default -> "Gemini API 요청에 실패했습니다 (HTTP " + exception.code() + ").";
		};
		return new IngestionException("GEMINI_API_ERROR", message, exception);
	}

	private void ensureConfigured() {
		if (client == null) {
			throw new IngestionException("GEMINI_NOT_CONFIGURED", "GOOGLE_API_KEY가 설정되지 않았습니다.");
		}
	}

	@PreDestroy
	void closeClient() {
		if (client != null) {
			client.close();
		}
	}

	public record SearchSource(String title, String url) {
	}
}

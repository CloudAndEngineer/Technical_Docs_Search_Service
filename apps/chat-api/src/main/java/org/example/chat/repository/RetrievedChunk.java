package org.example.chat.repository;

public record RetrievedChunk(
		String content,
		String documentTitle,
		String fileName,
		String sourceUrl,
		double similarity
) {
}

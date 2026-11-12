package org.example.ingestion.chunker;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FixedSizeOverlapChunker {

	private static final int CHUNK_SIZE = 1200;
	private static final int OVERLAP = 200;

	public List<String> chunk(String text) {
		List<String> chunks = new ArrayList<>();
		int start = 0;
		while (start < text.length()) {
			int end = Math.min(start + CHUNK_SIZE, text.length());
			if (end < text.length()) {
				int newline = text.lastIndexOf('\n', end);
				int space = text.lastIndexOf(' ', end);
				int boundary = Math.max(newline, space);
				if (boundary > start + CHUNK_SIZE / 2) {
					end = boundary;
				}
			}
			if (end < text.length() && end > start && Character.isHighSurrogate(text.charAt(end - 1))) {
				end--;
			}
			String chunk = text.substring(start, end).trim();
			if (!chunk.isEmpty()) {
				chunks.add(chunk);
			}
			if (end >= text.length()) {
				break;
			}
			start = Math.max(start + 1, end - OVERLAP);
		}
		return chunks;
	}
}

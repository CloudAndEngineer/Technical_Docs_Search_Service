package org.example.chat.dto.response;

public record ChatSourceResponse(
		int reference,
		String documentTitle,
		String fileName,
		String sourceUrl
) {
}

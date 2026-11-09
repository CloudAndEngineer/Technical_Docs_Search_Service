package org.example.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
		@NotBlank(message = "질문은 비어 있을 수 없습니다.")
		@Size(max = 4000, message = "질문은 4000자 이하여야 합니다.")
		String question
) {
}

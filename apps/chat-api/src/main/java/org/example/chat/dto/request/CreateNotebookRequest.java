package org.example.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNotebookRequest(
		@NotBlank(message = "노트북 제목은 비어 있을 수 없습니다.")
		@Size(max = 120, message = "노트북 제목은 120자 이하여야 합니다.")
		String title
) {
}

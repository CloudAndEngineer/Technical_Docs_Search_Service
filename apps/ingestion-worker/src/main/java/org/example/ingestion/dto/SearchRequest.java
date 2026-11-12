package org.example.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchRequest(
		@NotBlank(message = "검색할 기술 정보 이름을 입력하세요.")
		@Size(max = 300, message = "검색어는 300자 이내로 입력하세요.")
		String query
) {
}

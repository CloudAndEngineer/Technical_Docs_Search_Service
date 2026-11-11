package org.example.chat.dto.response;

import java.util.List;

public record ChatResponse(String answer, List<ChatSourceResponse> sources) {
}

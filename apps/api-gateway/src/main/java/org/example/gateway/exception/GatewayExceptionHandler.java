package org.example.gateway.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.example.gateway.dto.GatewayErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GatewayExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ResponseEntity<GatewayErrorResponse> handleGatewayException(
			Exception exception, HttpServletRequest request) {
		GatewayErrorResponse response = new GatewayErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				"GATEWAY_ERROR",
				"Gateway에서 요청을 처리하지 못했습니다.",
				request.getRequestURI());
		return ResponseEntity.badRequest().body(response);
	}
}

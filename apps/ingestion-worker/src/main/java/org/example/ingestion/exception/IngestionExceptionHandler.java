package org.example.ingestion.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.example.ingestion.dto.IngestionErrorResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;

@RestControllerAdvice
public class IngestionExceptionHandler {

	@ExceptionHandler(IngestionException.class)
	public ResponseEntity<IngestionErrorResponse> handleIngestion(
			IngestionException exception, HttpServletRequest request) {
		return badRequest(exception.getCode(), exception.getMessage(), request);
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
	public ResponseEntity<IngestionErrorResponse> handleValidation(
			Exception exception, HttpServletRequest request) {
		String message = exception instanceof MethodArgumentNotValidException validation
				? validation.getBindingResult().getFieldErrors().stream()
						.findFirst().map(DefaultMessageSourceResolvable::getDefaultMessage)
						.orElse("요청 값이 올바르지 않습니다.")
				: "요청 값이 올바르지 않습니다.";
		return badRequest("VALIDATION_ERROR", message, request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<IngestionErrorResponse> handleUploadSize(HttpServletRequest request) {
		return badRequest("FILE_TOO_LARGE", "업로드 파일의 크기가 허용 범위를 초과했습니다.", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<IngestionErrorResponse> handleUnexpected(HttpServletRequest request) {
		return badRequest("INVALID_REQUEST", "요청을 처리할 수 없습니다.", request);
	}

	private ResponseEntity<IngestionErrorResponse> badRequest(
			String code, String message, HttpServletRequest request) {
		return ResponseEntity.badRequest().body(new IngestionErrorResponse(
				Instant.now(), 400, code, message, request.getRequestURI()));
	}
}

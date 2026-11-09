package org.example.chat.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.example.chat.dto.response.ApiErrorResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
		return badRequest(exception.getCode(), exception.getMessage(), request);
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
	public ResponseEntity<ApiErrorResponse> handleValidation(Exception exception, HttpServletRequest request) {
		String message = exception instanceof MethodArgumentNotValidException validation
				? validation.getBindingResult().getFieldErrors().stream()
						.findFirst().map(DefaultMessageSourceResolvable::getDefaultMessage).orElse("요청 값이 올바르지 않습니다.")
					: "요청 값이 올바르지 않습니다.";
		return badRequest("VALIDATION_ERROR", message, request);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	public ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception exception, HttpServletRequest request) {
		return badRequest("INVALID_REQUEST", "요청 형식 또는 경로 값이 올바르지 않습니다.", request);
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ApiErrorResponse> handleDataAccess(DataAccessException exception, HttpServletRequest request) {
		return badRequest("DATABASE_ERROR", "데이터 저장소 처리 중 오류가 발생했습니다.", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
		return badRequest("INTERNAL_ERROR", "요청을 처리하는 중 오류가 발생했습니다.", request);
	}

	private ResponseEntity<ApiErrorResponse> badRequest(String code, String message, HttpServletRequest request) {
		ApiErrorResponse body = new ApiErrorResponse(
				Instant.now(), HttpStatus.BAD_REQUEST.value(), code, message, request.getRequestURI());
		return ResponseEntity.badRequest().body(body);
	}
}

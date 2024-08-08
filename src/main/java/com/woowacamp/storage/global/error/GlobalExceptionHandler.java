package com.woowacamp.storage.global.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.woowacamp.storage.global.response.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
		ErrorResponse errorResponse = ErrorResponse.of(e.getHttpStatus(), e.getMessage());
		return ResponseEntity.status(e.getHttpStatus()).body(errorResponse);
	}
}

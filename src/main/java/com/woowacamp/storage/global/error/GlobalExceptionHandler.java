package com.woowacamp.storage.global.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.woowacamp.storage.global.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
		ErrorResponse errorResponse = ErrorResponse.of(e.getHttpStatus(), e.getMessage());
		if (e.getDebugMessage() != null) {
			log.debug(e.getDebugMessage());
		}
		return ResponseEntity.status(e.getHttpStatus()).body(errorResponse);
	}
}

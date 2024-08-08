package com.woowacamp.storage.global.error;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ErrorCode {

	// 400

	// 500

	;

	private final HttpStatus status;
	private final String message;

	public CustomException baseException() {
		return new CustomException(status, message);
	}

	public CustomException baseException(String debugMessage, Object... args) {
		return new CustomException(status, message, String.format(debugMessage, args));
	}
}

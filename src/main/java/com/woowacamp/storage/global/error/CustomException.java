package com.woowacamp.storage.global.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

	private final HttpStatus httpStatus;
	private final String message;
	private final String debugMessage;

	public CustomException(HttpStatus httpStatus, String message) {
		super(createMessageForm(httpStatus, message, null));
		this.httpStatus = httpStatus;
		this.message = message;
		this.debugMessage = null;
	}

	public CustomException(HttpStatus httpStatus, String message, String debugMessage) {
		super(createMessageForm(httpStatus, message, debugMessage));
		this.httpStatus = httpStatus;
		this.message = message;
		this.debugMessage = debugMessage;
	}

	private static String createMessageForm(HttpStatus httpStatus, String message, String debugMessage) {
		StringBuilder detailMessage = new StringBuilder(httpStatus.toString()).append(": ").append(message);
		if (debugMessage != null && !debugMessage.isEmpty()) {
			detailMessage.append(", ").append(debugMessage);
		}
		return detailMessage.toString();
	}
}

package com.woowacamp.storage.global.error;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ErrorCode {

	// 400
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다."),
	FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다"),
	WRONG_CURSOR_TYPE(HttpStatus.BAD_REQUEST, "잘못된 커서타입입니다."),
	WRONG_FOLDER_CONTENTS_SORT_FIELD(HttpStatus.BAD_REQUEST, "잘못된 폴더 컨텐츠 정렬 기준입니다."),
	WRONG_PAGE_SIZE(HttpStatus.BAD_REQUEST, "잘못된 페이지 사이즈입니다.");

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

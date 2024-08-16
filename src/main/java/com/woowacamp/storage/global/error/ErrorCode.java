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
	WRONG_PAGE_SIZE(HttpStatus.BAD_REQUEST, "잘못된 페이지 사이즈입니다."),
	INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "파일 이름이 부적절합니다."),
	NO_PERMISSION(HttpStatus.FORBIDDEN, "권한이 부족합니다."),
	EXCEED_MAX_FILE_SIZE(HttpStatus.BAD_REQUEST, "요청 가능한 파일 크기를 초과했습니다."),
	EXCEED_MAX_STORAGE_SIZE(HttpStatus.BAD_REQUEST, "최대 저장 공간 크기를 초과했습니다."),
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "올바르지 않은 입력입니다."),
	FILE_METADATA_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 파일을 찾을 수 없습니다."),
	EXCEED_MAX_FOLDER_DEPTH(HttpStatus.BAD_REQUEST, "최대 폴더 깊이를 초과했습니다."),
	INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "요청 파일 사이즈와 실제 파일 사이즈가 일치하지 않습니다."),
	FILE_NAME_DUPLICATE(HttpStatus.CONFLICT,"파일 또는 폴더 이름이 중복되었습니다."),
	// 500,
	FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
	FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다.");
	private final HttpStatus status;
	private final String message;

	public CustomException baseException() {
		return new CustomException(status, message);
	}

	public CustomException baseException(String debugMessage, Object... args) {
		return new CustomException(status, message, String.format(debugMessage, args));
	}
}

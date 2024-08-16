package com.woowacamp.storage.domain.file.dto;

import java.util.Map;

import com.woowacamp.storage.global.error.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FormMetadataDto {
	private long userId;
	private long parentFolderId;
	private long fileSize;

	public static FormMetadataDto of(Map<String, String> formFields) {
		try {
			long userId = Long.parseLong(formFields.get("userId"));
			long parentFolderId = Long.parseLong(formFields.get("parentFolderId"));
			long fileSize = Long.parseLong(formFields.get("fileSize"));
			return new FormMetadataDto(userId, parentFolderId, fileSize);
		} catch (NumberFormatException exception) {
			throw ErrorCode.INVALID_INPUT_VALUE.baseException();
		}
	}
}

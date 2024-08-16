package com.woowacamp.storage.domain.file.dto;

public record FileMoveDto(
	long targetFolderId,
	long userId
) {
}

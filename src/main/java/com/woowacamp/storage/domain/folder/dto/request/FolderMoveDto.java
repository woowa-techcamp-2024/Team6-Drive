package com.woowacamp.storage.domain.folder.dto.request;

public record FolderMoveDto(
	long userId,
	long targetFolderId
) {
}

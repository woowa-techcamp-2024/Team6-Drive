package com.woowacamp.storage.domain.folder.dto.request;

import com.woowacamp.storage.global.annotation.CheckField;
import com.woowacamp.storage.global.aop.type.FieldType;

public record FolderMoveDto(
	@CheckField(FieldType.USER_ID) long userId,
	@CheckField(FieldType.MOVE_FOLDER_ID) long targetFolderId
) {
}

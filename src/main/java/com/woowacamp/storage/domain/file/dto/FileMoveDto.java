package com.woowacamp.storage.domain.file.dto;

import com.woowacamp.storage.global.annotation.CheckField;
import com.woowacamp.storage.global.aop.type.FieldType;

public record FileMoveDto(
	@CheckField(FieldType.MOVE_FOLDER_ID) long targetFolderId,
	@CheckField(FieldType.USER_ID) long userId
) {
}

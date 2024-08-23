package com.woowacamp.storage.domain.folder.dto.request;

import com.woowacamp.storage.global.annotation.CheckField;
import com.woowacamp.storage.global.aop.type.FieldType;

import jakarta.validation.constraints.Size;

public record CreateFolderReqDto(
	@CheckField(FieldType.USER_ID) long userId,
	@CheckField(FieldType.FOLDER_ID) long parentFolderId,
	@Size(min = 1, max = 100) String uploadFolderName,
	@CheckField(FieldType.CREATOR_ID) long creatorId
) {
}

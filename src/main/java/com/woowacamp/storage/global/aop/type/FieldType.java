package com.woowacamp.storage.global.aop.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FieldType {
	USER_ID("userId"), FILE_ID("fileId"), FOLDER_ID("folderId"), MOVE_FOLDER_ID("moveFolderId"), CREATOR_ID(
		"creatorId"),
	;
	private final String value;
}

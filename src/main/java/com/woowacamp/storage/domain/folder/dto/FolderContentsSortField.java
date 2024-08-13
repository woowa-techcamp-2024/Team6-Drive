package com.woowacamp.storage.domain.folder.dto;

import lombok.Getter;

@Getter
public enum FolderContentsSortField {
	CREATED_AT("createdAt"), FOLDER_SIZE("size");
	private final String value;

	FolderContentsSortField(String value) {
		this.value = value;
	}

	public static FolderContentsSortField fromValue(String value) {
		for (FolderContentsSortField field : FolderContentsSortField.values()) {
			if (field.getValue().equalsIgnoreCase(value)) {
				return field;
			}
		}
		throw new IllegalArgumentException("Invalid sort field: " + value);
	}
}

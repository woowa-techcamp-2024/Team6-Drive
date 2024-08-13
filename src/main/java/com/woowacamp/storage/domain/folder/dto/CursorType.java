package com.woowacamp.storage.domain.folder.dto;

import lombok.Getter;

@Getter
public enum CursorType {

	FOLDER("Folder"), FILE("File");
	private final String value;

	CursorType(String value) {
		this.value = value;
	}

	public static CursorType fromValue(String value) {
		for (CursorType field : CursorType.values()) {
			if (field.getValue().equalsIgnoreCase(value)) {
				return field;
			}
		}
		throw new IllegalArgumentException("Invalid sort field: " + value);
	}
}

package com.woowacamp.storage.domain.folder.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.woowacamp.storage.global.error.CustomException;

class FolderContentsSortFieldTest {

	@Test
	void fromValue_ShouldReturnEnum_WhenValidValueProvided() {
		// Valid cases
		assertEquals(FolderContentsSortField.CREATED_AT, FolderContentsSortField.fromValue("createdAt"));
		assertEquals(FolderContentsSortField.FOLDER_SIZE, FolderContentsSortField.fromValue("size"));
	}

	@Test
	void fromValue_ShouldThrowException_WhenInvalidValueProvided() {
		assertThrows(CustomException.class, () -> {
			FolderContentsSortField.fromValue("created_at");
		});
	}
}

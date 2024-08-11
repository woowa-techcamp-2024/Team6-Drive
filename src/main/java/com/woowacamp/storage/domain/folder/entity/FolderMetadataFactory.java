package com.woowacamp.storage.domain.folder.entity;

import java.time.LocalDateTime;

public class FolderMetadataFactory {
	public static FolderMetadata createFolderMetadata(LocalDateTime localDateTime, String folderName) {
		return FolderMetadata.builder()
			.createdAt(localDateTime)
			.updatedAt(localDateTime)
			.uploadFolderName(folderName)
			.build();
	}
}

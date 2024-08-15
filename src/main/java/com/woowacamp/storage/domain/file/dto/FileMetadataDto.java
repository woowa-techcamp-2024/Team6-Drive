package com.woowacamp.storage.domain.file.dto;

import com.woowacamp.storage.domain.file.entity.FileMetadata;

public record FileMetadataDto(Long metadataId, String uuid, Long ownerId, Long parentFolderId, long fileSize) {
	public static FileMetadataDto of(FileMetadata fileMetadata) {
		return new FileMetadataDto(fileMetadata.getId(), fileMetadata.getUuidFileName(), fileMetadata.getOwnerId(),
			fileMetadata.getParentFolderId(), fileMetadata.getFileSize());
	}
}

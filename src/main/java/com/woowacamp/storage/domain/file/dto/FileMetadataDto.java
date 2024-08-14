package com.woowacamp.storage.domain.file.dto;

import java.time.LocalDateTime;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.global.constant.UploadStatus;

public record FileMetadataDto(Long id, Long rootId, Long creatorId, String fileType, LocalDateTime createdAt,
							  LocalDateTime updatedAt, Long parentFolderId, Long size, String uploadFileName,
							  String uuidFileName, UploadStatus uploadStatus) {
	public static FileMetadataDto fromEntity(FileMetadata fileMetadata) {
		return new FileMetadataDto(fileMetadata.getId(), fileMetadata.getRootId(), fileMetadata.getCreatorId(),
			fileMetadata.getFileType(), fileMetadata.getCreatedAt(), fileMetadata.getUpdatedAt(),
			fileMetadata.getParentFolderId(), fileMetadata.getSize(), fileMetadata.getUploadFileName(),
			fileMetadata.getUuidFileName(), fileMetadata.getUploadStatus());
	}
}

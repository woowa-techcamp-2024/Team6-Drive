package com.woowacamp.storage.domain.file.entity;

import java.time.LocalDateTime;

import com.woowacamp.storage.domain.user.entity.User;
import com.woowacamp.storage.global.constant.UploadStatus;

public class FileMetadataFactory {

	public static FileMetadata buildInitialMetadata(User user, long parentFolderId, long fileSize, String uuidFileName,
		String fileName, String fileType, String thumbnailUUID) {
		LocalDateTime now = LocalDateTime.now();
		return FileMetadata.builder()
			.rootId(user.getRootFolderId())
			.creatorId(user.getId())
			.ownerId(user.getId())
			.parentFolderId(parentFolderId)
			.fileSize(fileSize)
			.uuidFileName(uuidFileName)
			.uploadStatus(UploadStatus.PENDING)
			.uploadFileName(fileName)
			.fileType(fileType)
			.createdAt(now)
			.updatedAt(now)
			.thumbnailUUID(thumbnailUUID)
			.build();
	}
}

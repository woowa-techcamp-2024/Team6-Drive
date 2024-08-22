package com.woowacamp.storage.domain.file.entity;

import java.time.LocalDateTime;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.user.entity.User;
import com.woowacamp.storage.global.constant.UploadStatus;

public class FileMetadataFactory {

	public static FileMetadata buildInitialMetadata(User user, FolderMetadata parentFolder, long fileSize,
		String uuidFileName,
		String fileName, String fileType) {
		LocalDateTime now = LocalDateTime.now();
		return FileMetadata.builder()
			.rootId(user.getRootFolderId())
			.creatorId(user.getId())
			.ownerId(parentFolder.getOwnerId())
			.parentFolderId(parentFolder.getId())
			.fileSize(fileSize)
			.uuidFileName(uuidFileName)
			.uploadStatus(UploadStatus.PENDING)
			.uploadFileName(fileName)
			.fileType(fileType)
			.createdAt(now)
			.updatedAt(now)
			.isShared(parentFolder.isShared())
			.sharingExpiredAt(parentFolder.getSharingExpiredAt())
			.permissionType(parentFolder.getPermissionType())
			.build();
	}
}

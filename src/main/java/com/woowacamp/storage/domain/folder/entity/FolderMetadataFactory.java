package com.woowacamp.storage.domain.folder.entity;

import java.time.LocalDateTime;

import com.woowacamp.storage.domain.folder.dto.request.CreateFolderReqDto;
import com.woowacamp.storage.domain.user.entity.User;
import com.woowacamp.storage.global.constant.CommonConstant;
import com.woowacamp.storage.global.constant.PermissionType;

public class FolderMetadataFactory {
	public static FolderMetadata createFolderMetadataBySignup(String folderName) {
		LocalDateTime now = LocalDateTime.now();
		return FolderMetadata.builder()
			.createdAt(now)
			.updatedAt(now)
			.uploadFolderName(folderName)
			.isShared(false)
			.sharingExpiredAt(CommonConstant.UNAVAILABLE_TIME)
			.permissionType(PermissionType.NONE)
			.build();
	}

	public static FolderMetadata createFolderMetadata(User user, FolderMetadata parentFolder,
		CreateFolderReqDto req) {
		LocalDateTime now = LocalDateTime.now();
		return FolderMetadata.builder()
			.rootId(user.getRootFolderId())
			.ownerId(parentFolder.getOwnerId())
			.creatorId(user.getId())
			.createdAt(now)
			.updatedAt(now)
			.parentFolderId(req.parentFolderId())
			.uploadFolderName(req.uploadFolderName())
			.isShared(parentFolder.isShared())
			.sharingExpiredAt(parentFolder.getSharingExpiredAt())
			.permissionType(parentFolder.getPermissionType())
			.build();
	}
}

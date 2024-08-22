package com.woowacamp.storage.domain.folder.entity;

import java.time.LocalDateTime;

import com.woowacamp.storage.domain.folder.dto.request.CreateFolderReqDto;
import com.woowacamp.storage.domain.user.entity.User;
import com.woowacamp.storage.global.constant.CommonConstant;

public class FolderMetadataFactory {
	public static FolderMetadata createFolderMetadataBySignup(LocalDateTime localDateTime, String folderName) {
		return FolderMetadata.builder()
			.createdAt(localDateTime)
			.updatedAt(localDateTime)
			.uploadFolderName(folderName)
			.isShared(false)
			.sharingExpiredAt(CommonConstant.SHARED_EXPIRED_AT)
			.build();
	}

	public static FolderMetadata createFolderMetadata(User user, LocalDateTime now, CreateFolderReqDto req) {
		return FolderMetadata.builder()
			.rootId(user.getRootFolderId())
			.ownerId(user.getId())
			.creatorId(user.getId())
			.createdAt(now)
			.updatedAt(now)
			.parentFolderId(req.parentFolderId())
			.uploadFolderName(req.uploadFolderName())
			.isShared(false)
			.sharingExpiredAt(CommonConstant.SHARED_EXPIRED_AT)
			.build();
	}
}

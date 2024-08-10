package com.woowacamp.storage.domain.user.entity;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.user.dto.request.CreateUserReqDto;

public class UserFactory {
	public static User createUser(CreateUserReqDto req, FolderMetadata rootFolder) {
		return User.builder().userName(req.getUserName()).rootFolderId(rootFolder.getId()).build();
	}
}

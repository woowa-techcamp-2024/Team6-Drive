package com.woowacamp.storage.domain.user.entity;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;

public class UserFactory {
	public static User createUser(String userName, FolderMetadata rootFolder) {
		return User.builder().userName(userName).rootFolderId(rootFolder.getId()).build();
	}
}

package com.woowacamp.storage.domain.user.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.domain.user.entity.User;
import com.woowacamp.storage.domain.user.entity.dto.UserDto;
import com.woowacamp.storage.domain.user.entity.dto.request.CreateUserReqDto;
import com.woowacamp.storage.domain.user.repository.UserRepository;
import com.woowacamp.storage.global.error.CustomException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private static final String rootFolderName = "rootFolder";
	private final UserRepository userRepository;
	private final FolderMetadataRepository folderMetadataRepository;

	public UserDto findById(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "잘못된 id 입니다"));

		return UserDto.builder()
			.userName(user.getUserName())
			.rootFolderId(user.getRootFolderId())
			.id(user.getId()).build();
	}

	@Transactional(readOnly = false)
	public UserDto save(CreateUserReqDto req) {
		LocalDateTime now = LocalDateTime.now();

		FolderMetadata rootFolder = folderMetadataRepository.save(FolderMetadata.builder()
			.createdAt(now)
			.updatedAt(now)
			.uploadFolderName(rootFolderName)
			.build());

		User user = userRepository.save(User.builder()
			.userName(req.getUserName())
			.rootFolderId(rootFolder.getId())
			.build());

		rootFolder.initOwnerId(user.getId());
		rootFolder.initCreatorId(user.getId());

		return UserDto.builder()
			.rootFolderId(user.getRootFolderId())
			.userName(user.getUserName())
			.id(user.getId()).build();
	}
}

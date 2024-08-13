package com.woowacamp.storage.domain.folder.service;

import static com.woowacamp.storage.domain.folder.entity.FolderMetadataFactory.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.woowacamp.storage.domain.folder.dto.request.CreateFolderReqDto;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.domain.user.entity.User;
import com.woowacamp.storage.domain.user.repository.UserRepository;
import com.woowacamp.storage.global.constant.CommonConstant;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FolderService {

	private final FolderMetadataRepository folderMetadataRepository;
	private final UserRepository userRepository;

	public void createFolder(CreateFolderReqDto req) {
		User user = userRepository.findById(req.userId())
			.orElseThrow(ErrorCode.USER_NOT_FOUND::baseException);

		// TODO: 이후 공유 기능이 생길 때, request에 ownerId, creatorId 따로 받아야함
		validatePermission(req);
		validateFolderName(req);
		validateFolder(req);
		LocalDateTime now = LocalDateTime.now();
		folderMetadataRepository.save(createFolderMetadata(user, now, req));
	}

	/**
	 * 같은 depth(부모 폴더가 같음)에 동일한 이름의 폴더가 있는지 확인
	 * 최대 depth가 50 이하인지 확인
	 */
	private void validateFolder(CreateFolderReqDto req) {
		if (folderMetadataRepository.existsByParentFolderIdAndUploadFolderName(req.parentFolderId(),
			req.uploadFolderName())) {
			throw ErrorCode.INVALID_FILE_NAME.baseException();
		}
		int depth = 1;
		Long currentFolderId = req.parentFolderId();
		while (true) {
			Optional<Long> parentFolderIdById = folderMetadataRepository.findParentFolderIdById(currentFolderId);
			if (parentFolderIdById.isEmpty()) {
				break;
			}
			currentFolderId = parentFolderIdById.get();
			depth++;
		}

		if (depth >= CommonConstant.MAX_FOLDER_DEPTH) {
			throw ErrorCode.EXCEED_MAX_FOLDER_DEPTH.baseException();
		}
	}

	/**
	 * 폴더 이름에 금칙어가 있는지 확인
	 */
	private static void validateFolderName(CreateFolderReqDto req) {
		if (Arrays.stream(CommonConstant.FILE_NAME_BLACK_LIST)
			.anyMatch(character -> req.uploadFolderName().indexOf(character) != -1)) {
			throw ErrorCode.INVALID_FILE_NAME.baseException();
		}
	}

	/**
	 * 부모 폴더가 요청한 사용자의 폴더인지 확인
	 */
	private void validatePermission(CreateFolderReqDto req) {
		if (!folderMetadataRepository.existsByIdAndCreatorId(req.parentFolderId(), req.userId())) {
			throw ErrorCode.NO_PERMISSION.baseException();
		}
	}
}

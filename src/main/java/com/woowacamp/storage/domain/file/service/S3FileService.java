package com.woowacamp.storage.domain.file.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.dto.FileMetadataDto;
import com.woowacamp.storage.domain.file.dto.PartContext;
import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.entity.FileMetadataFactory;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.domain.user.entity.User;
import com.woowacamp.storage.domain.user.repository.UserRepository;
import com.woowacamp.storage.global.constant.CommonConstant;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class S3FileService implements FileService {

	private static final Logger log = LoggerFactory.getLogger(S3FileService.class);
	private final FileMetadataRepository fileMetadataRepository;
	private final FolderMetadataRepository folderMetadataRepository;
	private final UserRepository userRepository;

	private static final long MAX_FILE_SIZE = 500 * 1024 * 1024;
	private static final long MAX_STORAGE_SIZE = 1024 * 1024 * 1024;

	@Transactional
	public FileMetadataDto createInitialMetadata(Map<String, String> formFields, PartContext partContext) {
		long userId;
		long parentFolderId;
		long fileSize;
		try {
			userId = Long.parseLong(formFields.get("userId"));
			parentFolderId = Long.parseLong(formFields.get("parentFolderId"));
			fileSize = Long.parseLong(formFields.get("fileSize"));
		} catch (NumberFormatException exception) {
			throw ErrorCode.INVALID_INPUT_VALUE.baseException();
		}

		String fileName = partContext.getCurrentFileName();
		String fileType = getFileTypeByFileName(fileName);
		User user = userRepository.findById(userId).orElseThrow(ErrorCode.USER_NOT_FOUND::baseException);

		validateFileSize(fileSize, user.getRootFolderId());
		validateFile(partContext, parentFolderId, fileName, fileType);
		validateParentFolder(parentFolderId, userId);

		String uuidFileName = getUuidFileName();
		log.info("uuidFileName: {}", uuidFileName);

		// 1차 메타데이터 생성
		// TODO: 공유 기능이 생길 때, creatorId, ownerId 따로
		FileMetadata fileMetadata = fileMetadataRepository.save(
			FileMetadataFactory.buildInitialMetadata(user, parentFolderId, fileSize, uuidFileName, fileName, fileType));

		return FileMetadataDto.of(fileMetadata);
	}

	private void validateFileSize(long fileSize, Long rootFolderId) {
		FolderMetadata rootFolderMetadata = folderMetadataRepository.findById(rootFolderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

		if (fileSize > MAX_FILE_SIZE) {
			throw ErrorCode.EXCEED_MAX_FILE_SIZE.baseException();
		}
		if (rootFolderMetadata.getSize() + fileSize > MAX_STORAGE_SIZE) {
			throw ErrorCode.EXCEED_MAX_STORAGE_SIZE.baseException();
		}
	}

	/**
	 *
	 * 사용자 정보는 로그인을 했다고 가정하고 사용했습니다.
	 */
	@Transactional
	public void finalizeMetadata(FileMetadataDto fileMetadataDto, long fileSize) {
		FileMetadata fileMetadata = fileMetadataRepository.findById(fileMetadataDto.metadataId())
			.orElseThrow(ErrorCode.FILE_METADATA_NOT_FOUND::baseException);

		LocalDateTime now = LocalDateTime.now();
		updateFolderMetadataStatus(fileMetadataDto, fileSize, now);

		fileMetadata.updateFileSize(fileSize);
		fileMetadata.updateFinishUploadStatus();
		fileMetadata.updateCreatedAt(now);
		fileMetadata.updateUpdatedAt(now);
	}

	private void updateFolderMetadataStatus(FileMetadataDto req, long fileSize, LocalDateTime now) {
		Long parentFolderId = req.parentFolderId();
		while (parentFolderId != null) {
			FolderMetadata folderMetadata = folderMetadataRepository.findById(parentFolderId)
				.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
			folderMetadata.addSize(fileSize);
			folderMetadata.updateUpdatedAt(now);
			parentFolderId = folderMetadata.getParentFolderId();
		}
	}

	private void validateParentFolder(long parentFolderId, long userId) {
		FolderMetadata folderMetadata = folderMetadataRepository.findById(parentFolderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
		if (!Objects.equals(folderMetadata.getCreatorId(), userId)) {
			throw ErrorCode.NO_PERMISSION.baseException();
		}
	}

	private String getUuidFileName() {
		String uuidFileName = UUID.randomUUID().toString();
		while (fileMetadataRepository.existsByUuidFileName(uuidFileName)) {
			uuidFileName = UUID.randomUUID().toString();
		}
		return uuidFileName;
	}

	private void validateFile(PartContext partContext, long parentFolderId, String fileName, String fileType) {
		// 파일 이름에 금칙어가 있는지 확인
		if (Arrays.stream(CommonConstant.FILE_NAME_BLACK_LIST)
			.anyMatch(character -> partContext.getCurrentFileName().indexOf(character) != -1)) {
			throw ErrorCode.INVALID_FILE_NAME.baseException();
		}
		// 확장자에 금칙어가 있는지 확인
		if (Arrays.stream(CommonConstant.FILE_NAME_BLACK_LIST)
			.anyMatch(character -> partContext.getCurrentFileName().indexOf(character) != -1)) {
			throw ErrorCode.INVALID_FILE_NAME.baseException();
		}
		// 이미 해당 폴더에 같은 이름의 파일이 존재하는지 확인
		if (fileMetadataRepository.existsByParentFolderIdAndUploadFileNameAndFileType(parentFolderId, fileName,
			fileType)) {
			throw ErrorCode.FILE_NAME_DUPLICATE.baseException();
		}
	}

	private String getFileTypeByFileName(String fileName) {
		String fileType = null;
		int index = fileName.lastIndexOf('.');
		if (index != -1) {
			fileType = fileName.substring(index);
		}
		return fileType;
	}
}

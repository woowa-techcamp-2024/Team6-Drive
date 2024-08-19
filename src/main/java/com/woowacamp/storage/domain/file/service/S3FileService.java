package com.woowacamp.storage.domain.file.service;

import static com.woowacamp.storage.global.error.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.woowacamp.storage.domain.file.dto.FileDataDto;
import com.woowacamp.storage.domain.file.dto.FileMetadataDto;
import com.woowacamp.storage.domain.file.dto.FormMetadataDto;
import com.woowacamp.storage.domain.file.dto.PartContext;
import com.woowacamp.storage.domain.file.dto.UploadState;
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
public class S3FileService {

	private final FileMetadataRepository fileMetadataRepository;
	private final FolderMetadataRepository folderMetadataRepository;
	private final UserRepository userRepository;
	private final AmazonS3 amazonS3;

	@Value("${file.request.maxFileSize}")
	private long MAX_FILE_SIZE;
	@Value("${file.request.maxStorageSize}")
	private long MAX_STORAGE_SIZE;

	/**
	 * 1차로 메타데이터를 생성하는 메소드.
	 * 사용자의 요청 데이터에 있는 사용자 정보, 상위 폴더 정보, 파일 사이즈의 정보를 저장
	 */
	@Transactional
	public FileMetadataDto createInitialMetadata(FormMetadataDto formMetadataDto, PartContext partContext) {
		String fileName = partContext.getCurrentFileName();
		String fileType = getFileTypeByFileName(fileName);
		User user = userRepository.findById(formMetadataDto.getUserId())
			.orElseThrow(ErrorCode.USER_NOT_FOUND::baseException);
		validateRequest(formMetadataDto, partContext, user, fileName, fileType);

		String uuidFileName = getUuidFileName();

		// 1차 메타데이터 생성
		// TODO: 공유 기능이 생길 때, creatorId, ownerId 따로
		FileMetadata fileMetadata = fileMetadataRepository.save(
			FileMetadataFactory.buildInitialMetadata(user, formMetadataDto.getParentFolderId(),
				formMetadataDto.getFileSize(), uuidFileName, fileName, fileType));

		return FileMetadataDto.of(fileMetadata);
	}

	/**
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

	/**
	 * 요청 폼 데이터의 fieldFileSize와 실제 파일 크기인 uploadFileSize가 치일한 지 확인하는 메소드
	 */
	public void checkMetadata(UploadState state) {
		long fieldFileSize = state.getFileMetadataDto().fileSize();
		long uploadedFileSize = state.getFileSize();
		if (fieldFileSize != uploadedFileSize) {
			throw ErrorCode.INVALID_FILE_SIZE.baseException();
		}
	}

	private void validateRequest(FormMetadataDto formMetadataDto, PartContext partContext, User user, String fileName,
		String fileType) {
		validateFileSize(formMetadataDto.getFileSize(), user.getRootFolderId());
		validateFile(partContext, formMetadataDto.getParentFolderId(), fileName, fileType);
		validateParentFolder(formMetadataDto.getParentFolderId(), formMetadataDto.getUserId());
	}

	private void validateFileSize(long fileSize, Long rootFolderId) {
		FolderMetadata rootFolderMetadata = folderMetadataRepository.findByIdWithLock(rootFolderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

		if (fileSize > MAX_FILE_SIZE) {
			throw ErrorCode.EXCEED_MAX_FILE_SIZE.baseException();
		}
		if (rootFolderMetadata.getSize() + fileSize > MAX_STORAGE_SIZE) {
			throw ErrorCode.EXCEED_MAX_STORAGE_SIZE.baseException();
		}
	}

	/**
	 * 현재 폴더에서 루트 폴더까지 모든 폴더에 대한 size, updatedAt을 갱신
	 */
	private void updateFolderMetadataStatus(FileMetadataDto req, long fileSize, LocalDateTime now) {
		Long parentFolderId = req.parentFolderId();
		while (parentFolderId != null) {
			FolderMetadata folderMetadata = folderMetadataRepository.findByIdWithLock(parentFolderId)
				.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
			folderMetadata.addSize(fileSize);
			folderMetadata.updateUpdatedAt(now);
			parentFolderId = folderMetadata.getParentFolderId();
		}
	}

	/**
	 * 요청한 parentFolderId가 자신의 폴더에 대한 id인지 확인
	 */
	private void validateParentFolder(long parentFolderId, long userId) {
		FolderMetadata folderMetadata = folderMetadataRepository.findById(parentFolderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
		if (!Objects.equals(folderMetadata.getCreatorId(), userId)) {
			throw ErrorCode.NO_PERMISSION.baseException();
		}
	}

	/**
	 * UUID를 생성해 이미 존재하는지 확인
	 */
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

	public FileDataDto downloadByS3(Long fileId, String bucketName, String uuidFileName) {
		FileMetadata fileMetadata = fileMetadataRepository.findById(fileId).orElseThrow(FILE_NOT_FOUND::baseException);
		S3Object s3Object = amazonS3.getObject(new GetObjectRequest(bucketName, uuidFileName));
		return new FileDataDto(FileMetadataDto.of(fileMetadata), s3Object.getObjectContent());
	}
}

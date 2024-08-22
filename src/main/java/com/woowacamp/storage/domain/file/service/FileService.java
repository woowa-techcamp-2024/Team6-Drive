package com.woowacamp.storage.domain.file.service;

import static com.woowacamp.storage.global.error.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.woowacamp.storage.domain.file.dto.FileMoveDto;
import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.domain.folder.utils.FolderSearchUtil;
import com.woowacamp.storage.global.constant.PermissionType;
import com.woowacamp.storage.global.constant.UploadStatus;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

	private final FileMetadataRepository fileMetadataRepository;
	private final FolderMetadataRepository folderMetadataRepository;
	private final FolderSearchUtil folderSearchUtil;
	private final AmazonS3 amazonS3;
	@Value("${cloud.aws.credentials.bucketName}")
	private String BUCKET_NAME;

	/**
	 * FileMetadata의 parentFolderId를 변경한다.
	 * source folder, target folder의 모든 정보를 수정한다.
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void moveFile(Long fileId, FileMoveDto dto) {
		FolderMetadata folderMetadata = folderMetadataRepository.findByIdForUpdate(dto.targetFolderId())
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
		if (!folderMetadata.getOwnerId().equals(dto.userId())) {
			throw ErrorCode.ACCESS_DENIED.baseException();
		}
		FileMetadata fileMetadata = fileMetadataRepository.findByIdForUpdate(fileId)
			.orElseThrow(ErrorCode.FILE_NOT_FOUND::baseException);
		validateMetadata(dto, fileMetadata);

		Set<FolderMetadata> sourcePath = folderSearchUtil.getPathToRoot(fileMetadata.getParentFolderId());
		Set<FolderMetadata> targetPath = folderSearchUtil.getPathToRoot(dto.targetFolderId());
		FolderMetadata commonAncestor = folderSearchUtil.getCommonAncestor(sourcePath, targetPath);
		folderSearchUtil.updateFolderPath(sourcePath, targetPath, commonAncestor, fileMetadata.getFileSize());
		fileMetadata.updateParentFolderId(dto.targetFolderId());
	}

	private void validateMetadata(FileMoveDto dto, FileMetadata fileMetadata) {
		if (fileMetadata.getUploadStatus() != UploadStatus.SUCCESS) {
			throw ErrorCode.FILE_NOT_FOUND.baseException();
		}
		if (fileMetadataRepository.existsByParentFolderIdAndUploadFileNameAndFileType(dto.targetFolderId(),
			fileMetadata.getUploadFileName(), fileMetadata.getFileType())) {
			throw ErrorCode.FILE_NAME_DUPLICATE.baseException();
		}
	}

	public FileMetadata getFileMetadataBy(Long fileId, Long userId) {
		FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
			.orElseThrow(ErrorCode.FILE_NOT_FOUND::baseException);

		if (!Objects.equals(fileMetadata.getCreatorId(), userId)) {
			throw ACCESS_DENIED.baseException();
		}
		return fileMetadata;
	}

	// private String BUCKET_NAME
	@Transactional
	public void deleteFile(Long fileId, Long userId) {
		FileMetadata fileMetadata = fileMetadataRepository.findByIdAndOwnerId(fileId, userId)
			.orElseThrow(ACCESS_DENIED::baseException);

		fileMetadataRepository.delete(fileMetadata);

		try {
			amazonS3.deleteObject(BUCKET_NAME, fileMetadata.getUuidFileName());
		} catch (AmazonS3Exception e) {
			throw ErrorCode.FILE_DELETE_FAILED.baseException();
		}
	}

	@Transactional
	public void updateShareStatus(Long fileId, PermissionType permissionType, LocalDateTime sharingExpireAt) {
		FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
			.orElseThrow(ErrorCode.FILE_NOT_FOUND::baseException);
		fileMetadata.updateShareStatus(permissionType, sharingExpireAt);
	}
}

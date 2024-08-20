package com.woowacamp.storage.domain.file.service;

import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.dto.FileMoveDto;
import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.utils.FolderSearchUtil;
import com.woowacamp.storage.global.constant.UploadStatus;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

	private final FileMetadataRepository fileMetadataRepository;
	private final FolderSearchUtil folderSearchUtil;

	/**
	 * FileMetadata의 parentFolderId를 변경한다.
	 * source folder, target folder의 모든 정보를 수정한다.
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void moveFile(Long fileId, FileMoveDto dto) {
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
			throw ErrorCode.ACCESS_DENIED.baseException();
		}
		return fileMetadata;
	}
}

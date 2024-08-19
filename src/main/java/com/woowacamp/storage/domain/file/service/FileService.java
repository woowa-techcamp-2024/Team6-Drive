package com.woowacamp.storage.domain.file.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.dto.FileMoveDto;
import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.global.constant.UploadStatus;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

	private final FileMetadataRepository fileMetadataRepository;
	private final AsyncMoveFileService asyncMoveFileService;

	/**
	 * FileMetadata의 parentFolderId를 변경한다.
	 * source folder, target folder의 모든 정보를 수정한다.
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void moveFile(Long fileId, FileMoveDto dto) {
		FileMetadata fileMetadata = fileMetadataRepository.findByIdForUpdate(fileId)
			.orElseThrow(ErrorCode.FILE_NOT_FOUND::baseException);
		validateMetadata(dto, fileMetadata);
		long prevParentFolderId = fileMetadata.getParentFolderId();
		fileMetadata.updateParentFolderId(dto.targetFolderId());

		asyncMoveFileService.moveFile(prevParentFolderId, dto.targetFolderId(), fileMetadata);
	}

	private void validateMetadata(FileMoveDto dto, FileMetadata fileMetadata) {
		if (!fileMetadata.getUploadStatus().equals(UploadStatus.SUCCESS)) {
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

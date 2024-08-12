package com.woowacamp.storage.domain.folder.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.domain.folder.dto.FolderContentsDto;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.global.constant.UploadStatus;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FolderService {
	private final FolderMetadataRepository folderMetadataRepository;
	private final FileMetadataRepository fileMetadataRepository;
	private final int MAX_FOLDER_DEPTH = 50;

	public FolderContentsDto getFolderContents(long folderId, Pageable pageable) {
		FolderMetadata folder = folderMetadataRepository.findById(folderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
		Page<FolderMetadata> subFolders = folderMetadataRepository
			.findByParentFolderId(folderId, pageable);

		int fileCount = pageable.getPageSize() - subFolders.getNumberOfElements();
		if (fileCount == 0) {
			return;
		}

		Pageable filePageable = PageRequest.of(pageable.getPageNumber() - subFolders.getTotalPages(),
			pageable.getPageSize(), pageable.getSort());
		Page<FileMetadata> files = fileMetadataRepository
			.findFilesByParentFolderIdAndUploadStatusIs(folderId, UploadStatus.SUCCESS, filePageable);
		long totalElements = subFolders.getTotalElements() + files.getTotalElements();
		int totalPages = (int)totalElements / pageable.getPageSize();
		return new FolderContentsDto(totalPages, );
	}

	public void checkFolderOwnedBy(long folderId, long userId) {
		FolderMetadata folderMetadata = folderMetadataRepository.findById(folderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

		if (!folderMetadata.getOwnerId().equals(userId)) {
			throw ErrorCode.ACCESS_DENIED.baseException();
		}
	}
}

package com.woowacamp.storage.domain.folder.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.domain.folder.dto.CursorType;
import com.woowacamp.storage.domain.folder.dto.FolderContentsDto;
import com.woowacamp.storage.domain.folder.dto.FolderContentsSortField;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FolderService {
	private static final long INITIAL_CURSOR_ID = 0L;

	private final FileMetadataRepository fileMetadataRepository;
	private final FolderMetadataRepository folderMetadataRepository;

	@Transactional(readOnly = true)
	public void checkFolderOwnedBy(long folderId, long userId) {
		FolderMetadata folderMetadata = folderMetadataRepository.findById(folderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

		if (!folderMetadata.getOwnerId().equals(userId)) {
			throw ErrorCode.ACCESS_DENIED.baseException();
		}
	}

	@Transactional(readOnly = true)
	public FolderContentsDto getFolderContents(Long folderId, Long cursorId, CursorType cursorType, int size,
		FolderContentsSortField sortBy, Sort.Direction sortDirection) {
		List<FolderMetadata> folders = new ArrayList<>();
		List<FileMetadata> files = new ArrayList<>();

		if (cursorType.equals(CursorType.FILE)) {
			files = fetchFiles(folderId, cursorId, size, sortBy, sortDirection);
		} else if (cursorType.equals(CursorType.FOLDER)) {
			folders = fetchFolders(folderId, cursorId, size, sortBy, sortDirection);
			if (folders.size() < size) {
				files = fetchFiles(folderId, INITIAL_CURSOR_ID, size - folders.size(), sortBy, sortDirection);
			}
		}

		return new FolderContentsDto(folders, files);
	}

	private List<FileMetadata> fetchFiles(Long folderId, Long cursorId, int size, FolderContentsSortField sortBy,
		Sort.Direction direction) {
		return fileMetadataRepository.selectFilesWithPagination(folderId, cursorId, sortBy, direction, size);
	}

	private List<FolderMetadata> fetchFolders(Long folderId, Long cursorId, int size, FolderContentsSortField sortBy,
		Sort.Direction direction) {
		return folderMetadataRepository.selectFoldersWithPagination(folderId, cursorId, sortBy, direction, size);
	}
}

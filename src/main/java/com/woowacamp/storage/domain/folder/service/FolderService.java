package com.woowacamp.storage.domain.folder.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
	private static final String FILE_CURSOR_TYPE = "File";
	private static final String FOLDER_CURSOR_TYPE = "Folder";
	private static final long INITIAL_CURSOR_ID = 0L;

	private final FileMetadataRepository fileMetadataRepository;
	private final FolderMetadataRepository folderMetadataRepository;

	public void checkFolderOwnedBy(long folderId, long userId) {
		FolderMetadata folderMetadata = folderMetadataRepository.findById(folderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

		if (!folderMetadata.getOwnerId().equals(userId)) {
			throw ErrorCode.ACCESS_DENIED.baseException();
		}
	}

	public FolderContentsDto getFolderContents(Long folderId, Long cursorId, String cursorType, int size, String sortBy,
		Sort.Direction sortDirection) {
		Sort sort = Sort.by(sortDirection, sortBy);
		List<FolderMetadata> folders = new ArrayList<>();
		List<FileMetadata> files = new ArrayList<>();

		if (FILE_CURSOR_TYPE.equalsIgnoreCase(cursorType)) {
			files = fetchFiles(folderId, cursorId, size, sort);
		} else {
			folders = fetchFolders(folderId, cursorId, size, sort);
			if (folders.size() < size) {
				files = fetchFiles(folderId, INITIAL_CURSOR_ID, size - folders.size(), sort);
			}
		}

		CursorInfo nextCursor = determineNextCursor(folders, files, size);
		return new FolderContentsDto(folders, files, nextCursor.id, nextCursor.type);
	}

	private List<FileMetadata> fetchFiles(Long folderId, Long cursorId, int size, Sort sort) {
		return fileMetadataRepository.findByParentFolderIdAndIdGreaterThanAndUploadStatus(folderId, cursorId,
			UploadStatus.SUCCESS, PageRequest.of(0, size, sort));
	}

	private List<FolderMetadata> fetchFolders(Long folderId, Long cursorId, int size, Sort sort) {
		return folderMetadataRepository.findByParentFolderIdAndIdGreaterThan(folderId, cursorId,
			PageRequest.of(0, size, sort));
	}

	private CursorInfo determineNextCursor(List<FolderMetadata> folders, List<FileMetadata> files, int requestedSize) {
		int totalItems = folders.size() + files.size();
		if (totalItems < requestedSize) {
			return new CursorInfo(null, null);
		}

		if (!files.isEmpty()) {
			return new CursorInfo(files.get(files.size() - 1).getId(), FILE_CURSOR_TYPE);
		} else {
			return new CursorInfo(folders.get(folders.size() - 1).getId(), FOLDER_CURSOR_TYPE);
		}
	}

	private record CursorInfo(Long id, String type) {
	}
}

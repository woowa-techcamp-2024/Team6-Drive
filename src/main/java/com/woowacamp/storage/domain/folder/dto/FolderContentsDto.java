package com.woowacamp.storage.domain.folder.dto;

import java.util.List;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;

public record FolderContentsDto(
	int totalPages,
	int currentPage,
	int totalElements,
	int pageSize,
	int currentElements,
	FolderMetadata folder,
	List<FolderMetadata> subFolders,
	List<FileMetadata> files) {
}


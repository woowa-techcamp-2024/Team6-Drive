package com.woowacamp.storage.domain.file.repository;

import java.util.List;

import org.springframework.data.domain.Sort;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.folder.dto.FolderContentsSortField;

public interface FileCustomRepository {
	List<FileMetadata> selectFilesWithPagination(long parentId, long cursorId, FolderContentsSortField sortBy,
		Sort.Direction direction, int limit);
}

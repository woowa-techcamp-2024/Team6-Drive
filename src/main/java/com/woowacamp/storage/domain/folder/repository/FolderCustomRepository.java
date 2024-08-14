package com.woowacamp.storage.domain.folder.repository;

import java.util.List;

import org.springframework.data.domain.Sort;

import com.woowacamp.storage.domain.folder.dto.FolderContentsSortField;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;

public interface FolderCustomRepository {
	List<FolderMetadata> selectFoldersWithPagination(long parentId, long cursorId,
		FolderContentsSortField sortBy, Sort.Direction direction, int limit);
}

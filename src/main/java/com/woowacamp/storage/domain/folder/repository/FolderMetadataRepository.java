package com.woowacamp.storage.domain.folder.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;

public interface FolderMetadataRepository extends JpaRepository<FolderMetadata, Long> {
	Page<FolderMetadata> findByParentFolderId(long folderId, Pageable pageable);

	int countByParentFolderId(long folderId);
}

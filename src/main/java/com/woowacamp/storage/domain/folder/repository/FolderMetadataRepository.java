package com.woowacamp.storage.domain.folder.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;

public interface FolderMetadataRepository extends JpaRepository<FolderMetadata, Long> {
	@Query(value = "SELECT f FROM FolderMetadata  f WHERE f.parentFolderId = :folderId AND f.id > :cursorId ")
	List<FolderMetadata> findFoldersByCursor(Long folderId, Long cursorId, Pageable pageable);
}

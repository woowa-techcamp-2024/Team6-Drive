package com.woowacamp.storage.domain.file.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.global.constant.UploadStatus;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
	@Query(value = "SELECT f FROM FileMetadata  f WHERE f.parentFolderId = :folderId AND f.id > :cursorId AND f.uploadStatus = :uploadStatus ")
	List<FileMetadata> findFilesByCursor(Long folderId, Long cursorId, UploadStatus uploadStatus, Pageable pageable);
}

package com.woowacamp.storage.domain.file.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.global.constant.UploadStatus;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
	Page<FileMetadata> findFilesByParentFolderIdAndUploadStatusIs(long folderId, UploadStatus uploadStatus, Pageable pageable);

	int countByParentFolderId(long folderId);
}

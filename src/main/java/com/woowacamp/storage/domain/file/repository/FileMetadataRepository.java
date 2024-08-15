package com.woowacamp.storage.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.entity.FileMetadata;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

	boolean existsByUuidFileName(String uuidFileName);

	boolean existsByParentFolderIdAndUploadFileNameAndFileType(Long parentFolderId, String uploadFileName,
		String fileType);

	@Transactional
	void deleteByUuidFileName(String uuidFileName);
}

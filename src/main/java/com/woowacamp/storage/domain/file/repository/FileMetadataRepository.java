package com.woowacamp.storage.domain.file.repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.global.constant.UploadStatus;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long>, FileCustomRepository {

	boolean existsByUuidFileName(String uuidFileName);

	boolean existsByParentFolderIdAndUploadFileNameAndFileType(Long parentFolderId, String uploadFileName,
		String fileType);

	@Transactional
	void deleteByUuidFileName(String uuidFileName);
	List<FileMetadata> findByParentFolderIdAndIdGreaterThanAndUploadStatus(Long folderId, Long cursorId,
		UploadStatus uploadStatus, PageRequest of);
}

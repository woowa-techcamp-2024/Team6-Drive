package com.woowacamp.storage.domain.file.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.entity.FileMetadata;

import jakarta.persistence.LockModeType;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long>, FileCustomRepository {

	boolean existsByUuidFileName(String uuidFileName);

	boolean existsByParentFolderIdAndUploadFileNameAndFileType(Long parentFolderId, String uploadFileName,
		String fileType);

	@Transactional
	void deleteByUuidFileName(String uuidFileName);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<FileMetadata> findByIdAndOwnerId(Long id, Long ownerId);
}

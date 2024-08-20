package com.woowacamp.storage.domain.file.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
	@Query(value = """
			select f from FileMetadata f where f.id = :id
		""")
	Optional<FileMetadata> findByIdForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
	@Query(value = """
			select f from FileMetadata f where f.parentFolderId = :parentFolderId
		""")
	List<FileMetadata> findByParentFolderIdForUpdate(@Param("parentFolderId") Long parentFolderId);
}

package com.woowacamp.storage.domain.file.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.global.constant.UploadStatus;

import jakarta.persistence.LockModeType;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long>, FileCustomRepository {

	boolean existsByUuidFileName(String uuidFileName);

	boolean existsByParentFolderIdAndUploadFileNameAndUploadStatusNot(Long parentFolderId, String uploadFileName,
		UploadStatus uploadStatus);

	@Transactional
	void deleteByUuidFileName(String uuidFileName);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = """
			select f from FileMetadata f where f.id = :id and f.uploadStatus != 'FAIL'
		""")
	Optional<FileMetadata> findByIdForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<FileMetadata> findByIdAndOwnerIdAndUploadStatusNot(Long id, Long ownerId, UploadStatus uploadStatus);

	// 부모 폴더에 락을 걸고 조회하는 메소드
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = """
			select f from FileMetadata f where f.parentFolderId=:parentFolderId and f.uploadStatus != 'FAIL'
		""")
	List<FileMetadata> findByParentFolderIdForUpdate(Long parentFolderId);

	@Modifying
	@Query("DELETE FROM FileMetadata f WHERE f.id IN :ids")
	void deleteAllByIdInBatch(@Param("ids") Iterable<Long> ids);

	// 부모 폴더에 락을 걸지 않고 조회하는 메소드
	List<FileMetadata> findByParentFolderIdAndUploadStatusNot(Long parentFolderId, UploadStatus uploadStatus);

	@Modifying
	@Query(value = """
			update FileMetadata f
			set f.fileSize = :fileSize, f.uploadStatus = :uploadStatus, f.createdAt = NOW(), f.updatedAt = NOW()
			where f.id = :fileId
		""")
	int finalizeMetadata(@Param("fileId") long fileId, @Param("fileSize") long fileSize,
		@Param("uploadStatus") UploadStatus uploadStatus);

	@Modifying
	@Query(value = """
			update FileMetadata f
			set f.parentFolderId = :newParentId
			where f.parentFolderId in :ids
		""")
	int updateParentFolderIdForDelete(@Param("newParentId") long newParentId, @Param("ids") Iterable<Long> ids);

	@Modifying
	@Query(value = """
			select * from file_metadata f
			where f.parent_folder_id = :orphanParentId limit 50;
		""", nativeQuery = true)
	List<FileMetadata> findOrphanFiles(@Param("orphanParentId") int orphanParentId);

	boolean existsByParentFolderIdAndUploadStatus(Long parentFolderId, UploadStatus uploadStatus);

	@Transactional
	@Modifying
	@Query("""
			update FileMetadata f set f.uploadStatus = 'FAIL', f.updatedAt = NOW() where f.id = :fileMetadataId
		""")
	void updateUploadStatusById(@Param("fileMetadataId") Long fileMetadataId);

	@Transactional
	@Modifying
	@Query("""
			update FileMetadata f set f.uploadStatus = 'FAIL', f.updatedAt = NOW() where f.uuidFileName = :uuid
		""")
	void updateUploadStatusByUuid(@Param("uuid") String uuid);

	@Transactional
	@Query(value = """
			select * from file_metadata f where f.upload_status = 'FAIL' limit 50;
		""", nativeQuery = true)
	List<FileMetadata> findFailedFileMetadata();

	@Lock(LockModeType.PESSIMISTIC_READ)
	@Query(value = """
			select f from FileMetadata f
			where f.id = :fileId
		""")
	Optional<FileMetadata> findByIdForShare(@Param("fileId") long fileId);

	List<FileMetadata> findByOwnerId(Long ownerId);
}

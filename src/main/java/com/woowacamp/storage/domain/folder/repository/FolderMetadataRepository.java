package com.woowacamp.storage.domain.folder.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;

import jakarta.persistence.LockModeType;

public interface FolderMetadataRepository extends JpaRepository<FolderMetadata, Long>, FolderCustomRepository {

	boolean existsByIdAndCreatorId(Long id, Long creatorId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	boolean existsByParentFolderIdAndUploadFolderName(Long parentFolderId, String uploadFolderName);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = """
			select f.parentFolderId from FolderMetadata f where f.id = :id
		""")
	Optional<Long> findParentFolderIdById(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT f FROM FolderMetadata f WHERE f.id = :id")
	Optional<FolderMetadata> findByIdForUpdate(long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = """
			select f.id from FolderMetadata f where f.parentFolderId = :parentFolderId
		""")
	List<Long> findIdsByParentFolderIdForUpdate(@Param("parentFolderId") Long parentFolderId);

}

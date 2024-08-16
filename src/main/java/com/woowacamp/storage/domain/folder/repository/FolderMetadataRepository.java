package com.woowacamp.storage.domain.folder.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;

import jakarta.persistence.LockModeType;

public interface FolderMetadataRepository extends JpaRepository<FolderMetadata, Long>, FolderCustomRepository {

	boolean existsByIdAndCreatorId(Long id, Long creatorId);

	boolean existsByParentFolderIdAndUploadFolderName(Long parentFolderId, String uploadFolderName);

	@Query(value = """
			select f.parentFolderId from FolderMetadata f where f.id = :id
		""")
	Optional<Long> findParentFolderIdById(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<FolderMetadata> findById(long id);
}

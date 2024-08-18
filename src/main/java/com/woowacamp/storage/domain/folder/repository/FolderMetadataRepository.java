package com.woowacamp.storage.domain.folder.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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
	@Query(value = """
			select f from FolderMetadata f  where f.id = :folderId
		""")
	Optional<FolderMetadata> findByIdForUpdate(Long folderId);

	// 부모 폴더에 락을 걸고 하위 폴더를 조회하는 메소드
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = """
			select f from FolderMetadata f where f.parentFolderId = :parentFolderId
		""")
	List<FolderMetadata> findByParentFolderIdForUpdate(Long parentFolderId);

	@Modifying
	@Query("DELETE FROM FolderMetadata f WHERE f.id IN :ids")
	void deleteAllByIdInBatch(@Param("ids") Iterable<Long> ids);

	// 부모 폴더에 락을 걸지 않고 하위 폴더를 조회하는 메소드
	List<FolderMetadata> findByParentFolderId(Long parentFolderId);

	@Modifying
	@Query(value = """
			update FolderMetadata f
			set f.parentFolderId = :newParentId
			where f.parentFolderId in :ids
		""")
	void updateParentFolderIdForDelete(@Param("newParentId") int newParentId,
		@Param("ids") Iterable<Long> folderIdListForDelete);
}

package com.woowacamp.storage.domain.folder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.woowacamp.storage.domain.folder.entity.FolderMoveFailureLog;

public interface FolderMoveFailureLogRepository extends JpaRepository<FolderMoveFailureLog, Long> {

	@Query(value = """
			select * from folder_move_failure_log limit 10
		""", nativeQuery = true)
	List<FolderMoveFailureLog> findAllWithPagination();
}

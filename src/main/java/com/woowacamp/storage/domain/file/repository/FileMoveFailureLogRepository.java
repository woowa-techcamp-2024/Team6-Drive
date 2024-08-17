package com.woowacamp.storage.domain.file.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.woowacamp.storage.domain.file.entity.FileMoveFailureLog;

public interface FileMoveFailureLogRepository extends JpaRepository<FileMoveFailureLog, Long> {

	@Query(value = """
			select * from file_move_failure_log limit 10
		""", nativeQuery = true)
	List<FileMoveFailureLog> findAllWithPagination();

}

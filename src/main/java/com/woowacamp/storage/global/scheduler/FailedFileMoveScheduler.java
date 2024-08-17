package com.woowacamp.storage.global.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.entity.FileMoveFailureLog;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.domain.file.repository.FileMoveFailureLogRepository;
import com.woowacamp.storage.domain.file.service.AsyncMoveFileService;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedFileMoveScheduler {

	private final FileMoveFailureLogRepository fileMoveFailureLogRepository;
	private final AsyncMoveFileService asyncMoveFileService;
	private final FileMetadataRepository fileMetadataRepository;

	@Scheduled(fixedDelay = 1000 * 30)
	@Transactional
	public void processFailedFileMove() {
		List<FileMoveFailureLog> fileMoveFailureLogs;
		do {
			fileMoveFailureLogs = fileMoveFailureLogRepository.findAllWithPagination();
			processBatch(fileMoveFailureLogs);
		} while (!fileMoveFailureLogs.isEmpty());
	}

	private void processBatch(List<FileMoveFailureLog> fileMoveFailureLogs) {
		for (FileMoveFailureLog fileMoveFailureLog : fileMoveFailureLogs) {
			FileMetadata fileMetadata = fileMetadataRepository.findById(fileMoveFailureLog.getFileId())
				.orElseThrow(ErrorCode.FILE_NOT_FOUND::baseException);
			asyncMoveFileService.moveFile(fileMoveFailureLog.getSourceFolderId(),
				fileMoveFailureLog.getTargetFolderId(), fileMetadata);
			fileMoveFailureLogRepository.deleteById(fileMoveFailureLog.getId());
			log.info("failureLogFileId: {}", fileMetadata.getId());
		}
	}
}

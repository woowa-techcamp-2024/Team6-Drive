package com.woowacamp.storage.global.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.entity.FolderMoveFailureLog;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.domain.folder.repository.FolderMoveFailureLogRepository;
import com.woowacamp.storage.domain.folder.service.AsyncMoveFolderService;
import com.woowacamp.storage.global.error.ErrorCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedFolderMoveScheduler {

	private final FolderMoveFailureLogRepository folderMoveFailureLogRepository;
	private final AsyncMoveFolderService asyncMoveFileService;
	private final FolderMetadataRepository folderMetadataRepository;

	@Scheduled(fixedDelay = 1000 * 30)
	@Transactional
	public void processFailedFileMove() {
		List<FolderMoveFailureLog> folderMoveFailureLogs;
		do {
			folderMoveFailureLogs = folderMoveFailureLogRepository.findAllWithPagination();
			processBatch(folderMoveFailureLogs);
		} while (!folderMoveFailureLogs.isEmpty());
	}

	private void processBatch(List<FolderMoveFailureLog> folderMoveFailureLogs) {
		for (FolderMoveFailureLog folderMoveFailureLog : folderMoveFailureLogs) {
			FolderMetadata folderMetadata = folderMetadataRepository.findById(folderMoveFailureLog.getId())
				.orElseThrow(ErrorCode.FILE_NOT_FOUND::baseException);
			asyncMoveFileService.moveFolder(folderMoveFailureLog.getSourceFolderId(),
				folderMoveFailureLog.getTargetFolderId(), folderMetadata);
			folderMoveFailureLogRepository.deleteById(folderMoveFailureLog.getId());
			log.info("failureLogFileId: {}", folderMetadata.getId());
		}
	}
}

package com.woowacamp.storage.domain.file.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.entity.FileMoveFailureLog;
import com.woowacamp.storage.domain.file.repository.FileMoveFailureLogRepository;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncMoveFileService {

	private final FileMoveFailureLogRepository fileMoveFailureLogRepository;
	private final FolderMetadataRepository folderMetadataRepository;
	private final ApplicationContext applicationContext;

	@Async("threadPoolTaskExecutor")
	public void moveFile(long sourceFolderId, long targetFolderId, FileMetadata fileMetadata) {
		AsyncMoveFileService proxy = applicationContext.getBean(AsyncMoveFileService.class);
		int retryCount = 0;
		while (retryCount < 3) {
			try {
				proxy.moveFileInternal(sourceFolderId, targetFolderId, fileMetadata);
				log.info("File {} moved successfully", fileMetadata.getId());
				return;
			} catch (Exception e) {
				retryCount++;
			}
		}
		proxy.saveFailureLog(fileMetadata.getId(), sourceFolderId, targetFolderId);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void moveFileInternal(long sourceFolderId, long targetFolderId, FileMetadata fileMetadata) {
		Set<FolderMetadata> sourcePath = getPathToRoot(sourceFolderId);
		Set<FolderMetadata> targetPath = getPathToRoot(targetFolderId);
		FolderMetadata commonAncestor = getCommonAncestor(sourcePath, targetPath);
		updateFolderPath(sourcePath, targetPath, commonAncestor, fileMetadata.getFileSize());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveFailureLog(Long fileId, Long sourceFolderId, Long targetFolderId) {
		FileMoveFailureLog failureLog = new FileMoveFailureLog(fileId, sourceFolderId, targetFolderId,
			LocalDateTime.now());
		fileMoveFailureLogRepository.save(failureLog);
		log.error("Failed to move file {} after 3 attempts. Failure log saved.", fileId);
	}

	/**
	 * sourceFolder부터 rootFolder까지, targetFoldder부터 rootFolder까지 모든 정보를 수정한다.
	 * sourceFolder부터 commonAncestorFolder 전까지는 folderSize와 updatedAt 수정
	 * targetFolder부터 commonAncestorFolder 전까지는 folderSize와 updatedAt 수정
	 * commonAncestorFolder부터 rootFolder 까지는 updatedAt만 수정
	 */
	private void updateFolderPath(Set<FolderMetadata> sourcePath, Set<FolderMetadata> targetPath,
		FolderMetadata commonAncestor, long fileSize) {
		LocalDateTime now = LocalDateTime.now();
		boolean isExistCommonAncestor = false;
		for (var source : sourcePath) {
			source.updateUpdatedAt(now);
			if (source.equals(commonAncestor)) {
				isExistCommonAncestor = true;
			}
			if (!isExistCommonAncestor) {
				source.addSize(-fileSize);
			}
		}
		for (var target : targetPath) {
			if (target.equals(commonAncestor)) {
				break;
			}
			target.addSize(fileSize);
			target.updateUpdatedAt(now);
		}
	}

	/**
	 * source, target path로부터 공통 조상 폴더를 구하는 함수
	 */
	private FolderMetadata getCommonAncestor(Set<FolderMetadata> sourcePath, Set<FolderMetadata> targetPath) {
		FolderMetadata commonAncestor = null;
		for (var source : sourcePath) {
			if (targetPath.contains(source)) {
				commonAncestor = source;
				break;
			}
		}
		return commonAncestor;
	}

	/**
	 * 현재 folder에서 rootFolder까지 경로를 구하는 함수
	 */
	private Set<FolderMetadata> getPathToRoot(Long folderId) {
		Set<FolderMetadata> path = new LinkedHashSet<>();
		FolderMetadata current = folderMetadataRepository.findById(folderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

		while (current != null) {
			path.add(current);
			if (current.getParentFolderId() == null) {
				break;
			}
			current = folderMetadataRepository.findById(current.getParentFolderId())
				.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
		}
		return path;
	}
}

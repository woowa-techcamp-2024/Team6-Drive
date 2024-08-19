package com.woowacamp.storage.domain.folder.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.entity.FolderMoveFailureLog;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.domain.folder.repository.FolderMoveFailureLogRepository;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AsyncMoveFolderService {

	private final FolderMetadataRepository folderMetadataRepository;
	private final ApplicationContext applicationContext;
	private final FolderMoveFailureLogRepository folderMoveFailureLogRepository;

	@Async("threadPoolTaskExecutor")
	public void moveFolder(long sourceParentFolderId, long targetParentFolderId, FolderMetadata folderMetadata) {
		AsyncMoveFolderService proxy = applicationContext.getBean(AsyncMoveFolderService.class);
		int retryCount = 0;
		while (retryCount < 3) {
			try {
				proxy.moveFolderInternal(sourceParentFolderId, targetParentFolderId, folderMetadata);
				log.info("folder {} moved successfully", folderMetadata.getId());
				return;
			} catch (Exception e) {
				retryCount++;
			}
		}
		proxy.saveFailureLog(folderMetadata.getId(), sourceParentFolderId, targetParentFolderId);
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void moveFolderInternal(long sourceFolderId, long targetFolderId, FolderMetadata folderMetadata) {
		Set<FolderMetadata> sourcePath = getPathToRoot(sourceFolderId);
		Set<FolderMetadata> targetPath = getPathToRoot(targetFolderId);
		FolderMetadata commonAncestor = getCommonAncestor(sourcePath, targetPath);
		updateFolderPath(sourcePath, targetPath, commonAncestor, folderMetadata.getSize());
	}

	@Transactional
	public void saveFailureLog(Long folderId, Long sourceFolderId, Long targetFolderId) {
		FolderMoveFailureLog failureLog = new FolderMoveFailureLog(sourceFolderId, targetFolderId);
		folderMoveFailureLogRepository.save(failureLog);
		log.error("Failed to move folder {} after 3 attempts. Failure log saved.", folderId);
	}

	/**
	 * 현재 folder에서 rootFolder까지 경로를 구하는 함수
	 */
	private Set<FolderMetadata> getPathToRoot(Long folderId) {
		Set<FolderMetadata> path = new LinkedHashSet<>();
		FolderMetadata current = folderMetadataRepository.findByIdForUpdate(folderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

		while (current != null) {
			path.add(current);
			if (current.getParentFolderId() == null) {
				break;
			}
			current = folderMetadataRepository.findByIdForUpdate(current.getParentFolderId())
				.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
		}
		return path;
	}

	/**
	 * sourceFolder부터 rootFolder까지, targetFoldder부터 rootFolder까지 모든 정보를 수정한다.
	 * sourceFolder부터 commonAncestorFolder 전까지는 folderSize와 updatedAt 수정
	 * targetFolder부터 commonAncestorFolder 전까지는 folderSize와 updatedAt 수정
	 * commonAncestorFolder부터 rootFolder 까지는 updatedAt만 수정
	 */
	private void updateFolderPath(Set<FolderMetadata> sourcePath, Set<FolderMetadata> targetPath,
		FolderMetadata commonAncestor, long folderSize) {
		LocalDateTime now = LocalDateTime.now();
		boolean isExistCommonAncestor = false;
		for (var source : sourcePath) {
			source.updateUpdatedAt(now);
			if (source.equals(commonAncestor)) {
				isExistCommonAncestor = true;
			}
			if (!isExistCommonAncestor) {
				source.addSize(-folderSize);
			}
		}
		for (var target : targetPath) {
			if (target.equals(commonAncestor)) {
				break;
			}
			target.addSize(folderSize);
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
}

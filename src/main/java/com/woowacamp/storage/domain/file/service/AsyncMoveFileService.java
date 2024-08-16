package com.woowacamp.storage.domain.file.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsyncMoveFileService {

	private final FolderMetadataRepository folderMetadataRepository;

	@Async
	@Transactional
	public void moveFile(long sourceFolderId, long targetFolderId, long fileSize) {
		Set<FolderMetadata> sourcePath = getPathToRoot(sourceFolderId);
		Set<FolderMetadata> targetPath = getPathToRoot(targetFolderId);
		FolderMetadata commonAncestor = getCommonAncestor(sourcePath, targetPath);
		updateFolderPath(sourcePath, targetPath, commonAncestor, fileSize);
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
	 * source, target path로부터 공통 폴더를 구하는 함수
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

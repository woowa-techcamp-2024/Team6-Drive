package com.woowacamp.storage.domain.folder.service;

import static com.woowacamp.storage.domain.folder.entity.FolderMetadataFactory.*;
import static com.woowacamp.storage.global.constant.CommonConstant.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.domain.folder.dto.CursorType;
import com.woowacamp.storage.domain.folder.dto.FolderContentsDto;
import com.woowacamp.storage.domain.folder.dto.FolderContentsSortField;
import com.woowacamp.storage.domain.folder.dto.request.CreateFolderReqDto;
import com.woowacamp.storage.domain.folder.dto.request.FolderMoveDto;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.domain.folder.utils.FolderSearchUtil;
import com.woowacamp.storage.domain.user.entity.User;
import com.woowacamp.storage.domain.user.repository.UserRepository;
import com.woowacamp.storage.global.constant.CommonConstant;
import com.woowacamp.storage.global.constant.UploadStatus;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FolderService {
	private static final long INITIAL_CURSOR_ID = 0L;

	private final FileMetadataRepository fileMetadataRepository;
	private final FolderMetadataRepository folderMetadataRepository;
	private final UserRepository userRepository;
	private final FolderSearchUtil folderSearchUtil;

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void checkFolderOwnedBy(long folderId, long userId) {
		FolderMetadata folderMetadata = folderMetadataRepository.findByIdForUpdate(folderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

		if (!folderMetadata.getOwnerId().equals(userId)) {
			throw ErrorCode.ACCESS_DENIED.baseException();
		}
	}

	@Transactional(readOnly = true)
	public FolderContentsDto getFolderContents(Long folderId, Long cursorId, CursorType cursorType, int limit,
		FolderContentsSortField sortBy, Sort.Direction sortDirection, LocalDateTime dateTime, Long size) {
		List<FolderMetadata> folders = new ArrayList<>();
		List<FileMetadata> files = new ArrayList<>();

		if (cursorType.equals(CursorType.FILE)) {
			files = fetchFiles(folderId, cursorId, limit, sortBy, sortDirection, dateTime, size);
		} else if (cursorType.equals(CursorType.FOLDER)) {
			folders = fetchFolders(folderId, cursorId, limit, sortBy, sortDirection, dateTime, size);
			if (folders.size() < limit) {
				files = fetchFiles(folderId, INITIAL_CURSOR_ID, limit - folders.size(), sortBy, sortDirection, dateTime,
					size);
			}
		}

		return new FolderContentsDto(folders, files);
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void moveFolder(Long sourceFolderId, FolderMoveDto dto) {
		FolderMetadata folderMetadata = folderMetadataRepository.findByIdForUpdate(sourceFolderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
		validateMoveFolder(sourceFolderId, dto, folderMetadata);

		Set<FolderMetadata> sourcePath = folderSearchUtil.getPathToRoot(sourceFolderId);
		Set<FolderMetadata> targetPath = folderSearchUtil.getPathToRoot(dto.targetFolderId());
		FolderMetadata commonAncestor = folderSearchUtil.getCommonAncestor(sourcePath, targetPath);
		folderSearchUtil.updateFolderPath(sourcePath, targetPath, commonAncestor, folderMetadata.getSize());
		folderMetadata.updateParentFolderId(dto.targetFolderId());
	}

	private void validateMoveFolder(Long sourceFolderId, FolderMoveDto dto, FolderMetadata folderMetadata) {
		validateInvalidMove(dto, folderMetadata);
		validateFolderDepth(sourceFolderId, dto);
		validateDuplicatedFolderName(dto, folderMetadata);
	}

	/**
	 * root folder를 이동하려하는지 확인
	 * 같은 폴더 내에서 이동하려하는지 확인
	 */
	private void validateInvalidMove(FolderMoveDto dto, FolderMetadata folderMetadata) {
		if (Objects.equals(folderMetadata.getId(), dto.targetFolderId())) {
			throw ErrorCode.FOLDER_MOVE_NOT_AVAILABLE.baseException();
		}
		if (folderMetadata.getParentFolderId() == null) {
			throw ErrorCode.FOLDER_MOVE_NOT_AVAILABLE.baseException();
		}
		if (Objects.equals(folderMetadata.getParentFolderId(), dto.targetFolderId())) {
			throw ErrorCode.FOLDER_MOVE_NOT_AVAILABLE.baseException();
		}
	}

	/**
	 * sourceFolder의 최대 깊이 + 이동하려는 폴더의 깊이가 50을 넘는지 확인
	 */
	private void validateFolderDepth(Long sourceFolderId, FolderMoveDto dto) {
		int sourceFolderLeafDepth = getLeafDepth(sourceFolderId, 1, dto.targetFolderId());
		int targetFolderCurrentDepth = folderSearchUtil.getFolderDepth(dto.targetFolderId());
		if (sourceFolderLeafDepth + targetFolderCurrentDepth > MAX_FOLDER_DEPTH) {
			throw ErrorCode.EXCEED_MAX_FOLDER_DEPTH.baseException();
		}
	}

	/**
	 * currentFolderId로부터 최대 깊이를 구하는 dfs
	 * 이 과정 중, targetFolderId가 포함돼 있으면 예외 발생
	 * 깊이를 보는 과정이기 때문에, PENDING 상태의 file이 있어도 상관하지 않음
	 */
	private int getLeafDepth(long currentFolderId, int currentDepth, long targetFolderId) {
		List<Long> childFolderIds = folderMetadataRepository.findIdsByParentFolderIdForUpdate(currentFolderId);
		if (isExistsPendingFile(currentFolderId)) {
			throw ErrorCode.CANNOT_MOVE_FOLDER_WHEN_UPLOADING.baseException();
		}
		if (childFolderIds.isEmpty()) {
			return currentDepth;
		}
		int result = 0;
		for (Long childFolderId : childFolderIds) {
			if (Objects.equals(childFolderId, targetFolderId)) {
				throw ErrorCode.FOLDER_MOVE_NOT_AVAILABLE.baseException();
			}
			result = Math.max(result, getLeafDepth(childFolderId, currentDepth + 1, targetFolderId));
		}
		return result;
	}

	private boolean isExistsPendingFile(long currentFolderId) {
		return fileMetadataRepository.findByParentFolderIdForUpdate(currentFolderId).stream()
			.anyMatch(childFile -> childFile.getUploadStatus() == UploadStatus.PENDING);
	}

	/**
	 * 같은 폴더 내에 동일한 이름의 폴더가 있는지 확인
	 */
	private void validateDuplicatedFolderName(FolderMoveDto dto, FolderMetadata folderMetadata) {
		if (folderMetadataRepository.existsByParentFolderIdAndUploadFolderName(dto.targetFolderId(),
			folderMetadata.getUploadFolderName())) {
			throw ErrorCode.FILE_NAME_DUPLICATE.baseException();
		}
	}

	private List<FileMetadata> fetchFiles(Long folderId, Long cursorId, int limit, FolderContentsSortField sortBy,
		Sort.Direction direction, LocalDateTime dateTime, Long size) {
		return fileMetadataRepository.selectFilesWithPagination(folderId, cursorId, sortBy, direction, limit, dateTime,
			size);
	}

	private List<FolderMetadata> fetchFolders(Long folderId, Long cursorId, int limit, FolderContentsSortField sortBy,
		Sort.Direction direction, LocalDateTime dateTime, Long size) {
		return folderMetadataRepository.selectFoldersWithPagination(folderId, cursorId, sortBy, direction, limit,
			dateTime, size);
	}

	public void createFolder(CreateFolderReqDto req) {
		User user = userRepository.findById(req.userId()).orElseThrow(ErrorCode.USER_NOT_FOUND::baseException);

		// TODO: 이후 공유 기능이 생길 때, request에 ownerId, creatorId 따로 받아야함
		validatePermission(req);
		validateFolderName(req);
		validateFolder(req);
		LocalDateTime now = LocalDateTime.now();
		folderMetadataRepository.save(createFolderMetadata(user, now, req));
	}

	/**
	 * 같은 depth(부모 폴더가 같음)에 동일한 이름의 폴더가 있는지 확인
	 * 최대 depth가 50 이하인지 확인
	 */
	private void validateFolder(CreateFolderReqDto req) {
		if (folderMetadataRepository.existsByParentFolderIdAndUploadFolderName(req.parentFolderId(),
			req.uploadFolderName())) {
			throw ErrorCode.INVALID_FILE_NAME.baseException();
		}
		if (folderSearchUtil.getFolderDepth(req.parentFolderId()) >= MAX_FOLDER_DEPTH) {
			throw ErrorCode.EXCEED_MAX_FOLDER_DEPTH.baseException();
		}
	}

	/**
	 * 부모 폴더가 요청한 사용자의 폴더인지 확인
	 */
	private void validatePermission(CreateFolderReqDto req) {
		if (!folderMetadataRepository.existsByIdAndCreatorId(req.parentFolderId(), req.userId())) {
			throw ErrorCode.NO_PERMISSION.baseException();
		}
	}

	/**
	 * 폴더 이름에 금칙어가 있는지 확인
	 */
	private static void validateFolderName(CreateFolderReqDto req) {
		if (Arrays.stream(CommonConstant.FILE_NAME_BLACK_LIST)
			.anyMatch(character -> req.uploadFolderName().indexOf(character) != -1)) {
			throw ErrorCode.INVALID_FILE_NAME.baseException();
		}
	}
}

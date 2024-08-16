package com.woowacamp.storage.domain.folder.service;

import static com.woowacamp.storage.domain.folder.entity.FolderMetadataFactory.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.domain.folder.dto.CursorType;
import com.woowacamp.storage.domain.folder.dto.FolderContentsDto;
import com.woowacamp.storage.domain.folder.dto.FolderContentsSortField;
import com.woowacamp.storage.domain.folder.dto.request.CreateFolderReqDto;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.domain.user.entity.User;
import com.woowacamp.storage.domain.user.repository.UserRepository;
import com.woowacamp.storage.global.constant.CommonConstant;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FolderService {
	private static final long INITIAL_CURSOR_ID = 0L;

	private final FileMetadataRepository fileMetadataRepository;
	private final FolderMetadataRepository folderMetadataRepository;
	private final UserRepository userRepository;

	/**
	 * 폴더 이름에 금칙어가 있는지 확인
	 */
	private static void validateFolderName(CreateFolderReqDto req) {
		if (Arrays.stream(CommonConstant.FILE_NAME_BLACK_LIST)
			.anyMatch(character -> req.uploadFolderName().indexOf(character) != -1)) {
			throw ErrorCode.INVALID_FILE_NAME.baseException();
		}
	}

	@Transactional(readOnly = true)
	public void checkFolderOwnedBy(long folderId, long userId) {
		FolderMetadata folderMetadata = folderMetadataRepository.findById(folderId)
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
		validateFolderDepth(req);
	}

	/**
	 * 폴더를 무제한 생성하는 것을 방지하기 위해 깊이를 확인하는 메소드
	 */
	private void validateFolderDepth(CreateFolderReqDto req) {
		int depth = 1;
		Long currentFolderId = req.parentFolderId();
		while (true) {
			Optional<Long> parentFolderIdById = folderMetadataRepository.findParentFolderIdById(currentFolderId);
			if (parentFolderIdById.isEmpty()) {
				break;
			}
			currentFolderId = parentFolderIdById.get();
			depth++;
		}
		if (depth >= CommonConstant.MAX_FOLDER_DEPTH) {
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
}

package com.woowacamp.storage.domain.folder.service;

import static com.woowacamp.storage.domain.folder.entity.FolderMetadataFactory.*;
import static com.woowacamp.storage.global.constant.CommonConstant.*;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
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
	private final AmazonS3 amazonS3;
	@Value("${cloud.aws.credentials.bucketName}")
	private String BUCKET_NAME;

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
		return fileMetadataRepository.existsByParentFolderIdAndUploadStatus(currentFolderId, UploadStatus.PENDING);
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

	/**
	 * 부모 폴더를 삭제중인 경우가 있어서 for update로 부모 폴더를 조회합니다.
	 * 이미 제거되어 Null을 리턴한 경우 폴더가 생성되지 않습니다.
	 */
	@Transactional
	public Long createFolder(CreateFolderReqDto req) {
		User user = userRepository.findById(req.userId()).orElseThrow(ErrorCode.USER_NOT_FOUND::baseException);

		long parentFolderId = req.parentFolderId();
		long userId = req.userId();
		FolderMetadata parentFolder = folderMetadataRepository.findByIdForUpdate(parentFolderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

		validatePermission(parentFolder, userId);
		validateFolderName(req);
		validateFolder(req);
		FolderMetadata newFolder = folderMetadataRepository.save(createFolderMetadata(user, parentFolder, req));
		return newFolder.getId();
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
	private void validatePermission(FolderMetadata folderMetadata, long userId) {
		if (!folderMetadata.getOwnerId().equals(userId)) {
			throw ErrorCode.ACCESS_DENIED.baseException();
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

	/**
	 * 폴더 삭제 메소드입니다.
	 * 하위 폴더 및 파일까지 탐색하여 삭제를 진행합니다.
	 * deleteWithDfs 메소드를 통해 삭제해야 할 pk를 받아옵니다.
	 * 이후 pk 데이터를 바탕으로 폴더 및 파일 삭제와 S3에 미처 업로드가 되지 못한 파일의 부모 폴더의 값을 -1로 변경합니다.
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void deleteFolder(Long folderId, Long userId) {
		FolderMetadata folderMetadata = folderMetadataRepository.findByIdForUpdate(folderId)
			.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

		if (!folderMetadata.getOwnerId().equals(userId)) {
			throw ErrorCode.ACCESS_DENIED.baseException();
		}

		// 부모 폴더 정보가 없으면 루트 폴더를 제거하는 요청으로 예외를 반환한다.
		if (folderMetadata.getParentFolderId() == null) {
			throw ErrorCode.INVALID_DELETE_REQUEST.baseException();
		}

		List<Long> folderIdListForDelete = new ArrayList<>();
		List<Long> fileIdListForUpdate = new ArrayList<>();
		List<Long> fileIdListForDelete = new ArrayList<>();

		deleteWithDfs(folderId, folderIdListForDelete, fileIdListForDelete, fileIdListForUpdate);
		// deleteWithBfs(parentFolderId, folderIdListForDelete, fileIdListForDelete);

		if (!folderIdListForDelete.isEmpty()) {
			folderMetadataRepository.deleteAllByIdInBatch(folderIdListForDelete);
			folderMetadataRepository.updateParentFolderIdForDelete(CommonConstant.ORPHAN_PARENT_ID,
				fileIdListForUpdate);
			fileMetadataRepository.updateParentFolderIdForDelete(CommonConstant.ORPHAN_PARENT_ID,
				folderIdListForDelete);
		}
		if (!fileIdListForDelete.isEmpty()) {
			fileMetadataRepository.deleteAllByIdInBatch(fileIdListForDelete);
		}

		Long currentFolderId = folderMetadata.getParentFolderId();
		long fileSize = folderMetadata.getSize();
		LocalDateTime now = LocalDateTime.now();
		while (currentFolderId != null) {
			FolderMetadata currentFolderMetadata = folderMetadataRepository.findByIdForUpdate(currentFolderId)
				.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
			currentFolderMetadata.addSize(-fileSize);
			currentFolderMetadata.updateUpdatedAt(now);
			currentFolderId = currentFolderMetadata.getParentFolderId();
		}
	}

	/**
	 * 재귀호출을 하지 않고 stack을 사용한 DFS를 사용합니다.
	 * 깊이 우선으로 탐색하여 폴더와 파일을 삭제합니다.
	 */
	private void deleteWithDfs(long folderId, List<Long> folderIdListForDelete,
		List<Long> fileIdListForDelete, List<Long> fileIdListForUpdate) {
		Stack<Long> folderIdStack = new Stack<>();
		folderIdStack.push(folderId);

		// 재귀 탐색하며 S3 파일 삭제, 삭제해야하는 메타데이터 List에 저장하며 BatchSize 만큼 삭제
		while (!folderIdStack.isEmpty()) {
			Long currentFolderId = folderIdStack.pop();
			// 폴더아이디 삭제 목록에 추가
			folderIdListForDelete.add(currentFolderId);

			// 하위의 파일 조회
			List<FileMetadata> childFileMetadata = fileMetadataRepository.findByParentFolderId(
				currentFolderId);

			// 하위 파일의 실제 데이터 삭제 및 삭제해야 할 파일 id 값 저장
			childFileMetadata.forEach(fileMetadata -> {
				if (Objects.equals(fileMetadata.getUploadStatus(), UploadStatus.PENDING)) {
					throw ErrorCode.CANNOT_DELETE_FILE_WHEN_UPLOADING.baseException();
				}
				try {
					// s3에 파일 chunk를 쓰고 있을 수 있기 때문에 우선 조회 후 삭제 작업을 진행한다.
					amazonS3.getObjectMetadata(BUCKET_NAME, fileMetadata.getUuidFileName());
					amazonS3.deleteObject(BUCKET_NAME, fileMetadata.getUuidFileName());
					fileIdListForDelete.add(fileMetadata.getId());
					fileIdListForUpdate.add(fileMetadata.getId());
				} catch (AmazonS3Exception e) {
					e.printStackTrace();
					// 예외가 발생한 경우 해당 파일의 부모 폴더 필드를 -1로 만들어 준다.
					fileIdListForUpdate.add(fileMetadata.getId());
				}
			});

			// 하위의 폴더 조회
			List<FolderMetadata> childFolders = folderMetadataRepository.findByParentFolderId(currentFolderId);

			// 하위 폴더들을 스택에 추가
			for (FolderMetadata childFolder : childFolders) {
				folderIdStack.push(childFolder.getId());
			}
		}
	}

	/**
	 * 이후에 DFS와 비교를 위한 BFS를 활용한 파일 제거 메소드입니다.
	 */
	private void deleteWithBfs(long parentFolderId, List<Long> folderIdListForDelete, List<Long> fileIdListForDelete) {
		Queue<Long> folderIdQueue = new ArrayDeque<>();
		folderIdQueue.offer(parentFolderId);

		while (!folderIdQueue.isEmpty()) {
			Long currentFolderId = folderIdQueue.poll();

			folderIdListForDelete.add(currentFolderId);

			// 하위의 파일 삭제
			List<FileMetadata> childFiles = fileMetadataRepository.findByParentFolderIdForUpdate(currentFolderId);
			childFiles.forEach(fileMetadata -> {
				amazonS3.deleteObject(BUCKET_NAME, fileMetadata.getUuidFileName());
				fileIdListForDelete.add(fileMetadata.getId());
			});

			// 하위의 폴더 조회
			List<FolderMetadata> childFolder = folderMetadataRepository.findByParentFolderIdForUpdate(currentFolderId);

			// 다음 연산을 위해 Queue 에 offer
			childFolder.stream().forEach(folder -> {
				folderIdQueue.offer(folder.getId());
			});
		}
	}
}

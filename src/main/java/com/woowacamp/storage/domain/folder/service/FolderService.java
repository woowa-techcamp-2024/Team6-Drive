package com.woowacamp.storage.domain.folder.service;

import static com.woowacamp.storage.domain.folder.entity.FolderMetadataFactory.*;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Stack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
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
	private final AmazonS3 amazonS3;
	@Value("${cloud.aws.credentials.bucketName}")
	private String BUCKET_NAME;

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

	@Transactional
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
		List<Long> fileIdListForDelete = new ArrayList<>();

		Long parentFolderId = folderMetadata.getId();

		// deleteWithDfs(parentFolderId, folderIdListForDelete, fileIdListForDelete);
		deleteWithBfs(parentFolderId, folderIdListForDelete, fileIdListForDelete);

		if (!folderIdListForDelete.isEmpty()) {
			folderMetadataRepository.deleteAllByIdInBatch(folderIdListForDelete);
		}
		if (!fileIdListForDelete.isEmpty()) {
			fileMetadataRepository.deleteAllByIdInBatch(fileIdListForDelete);
		}
	}

	/**
	 * 재귀호출을 하지 않고 stack을 사용한 DFS를 사용합니다.
	 * 깊이 우선으로 탐색하여 폴더와 파일을 삭제합니다.
	 */
	private void deleteWithDfs(long parentFolderId, List<Long> folderIdListForDelete, List<Long> fileIdListForDelete){
		Stack<Long> folderIdStack = new Stack<>();
		folderIdStack.push(parentFolderId);

		// 재귀 탐색하며 S3 파일 삭제, 삭제해야하는 메타데이터 List에 저장하며 BatchSize 만큼 삭제
		while (!folderIdStack.isEmpty()) {
			Long currentFolderId = folderIdStack.pop();
			// 폴더아이디 삭제 목록에 추가
			folderIdListForDelete.add(currentFolderId);

			// 하위의 파일 조회
			List<FileMetadata> childFileMetadata = fileMetadataRepository.findByParentFolderIdForUpdate(
				currentFolderId);

			// 하위 파일의 실제 데이터 삭제 및 삭제해야 할 파일 id 값 저장
			childFileMetadata.forEach(fileMetadata -> {
				try {
					amazonS3.deleteObject(BUCKET_NAME, fileMetadata.getUuidFileName());
					fileIdListForDelete.add(fileMetadata.getId());
				} catch (AmazonS3Exception e) {
					e.printStackTrace();
				}
			});

			// 하위의 폴더 조회
			List<FolderMetadata> childFolders = folderMetadataRepository.findByParentFolderIdForUpdate(currentFolderId);

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
				amazonS3.deleteObject(BUCKET_NAME,fileMetadata.getUuidFileName());
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

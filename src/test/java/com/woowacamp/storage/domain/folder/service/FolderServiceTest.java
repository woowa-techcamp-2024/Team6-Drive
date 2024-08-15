package com.woowacamp.storage.domain.folder.service;

import static com.woowacamp.storage.domain.folder.dto.CursorType.FILE;
import static com.woowacamp.storage.domain.folder.dto.CursorType.*;
import static com.woowacamp.storage.domain.folder.dto.FolderContentsSortField.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.Sort.Direction.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.domain.folder.dto.FolderContentsDto;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.global.constant.UploadStatus;

@SpringBootTest
@ActiveProfiles("test")
class FolderServiceTest {

	@Autowired
	private FolderMetadataRepository folderMetadataRepository;

	@Autowired
	private FileMetadataRepository fileMetadataRepository;

	@Autowired
	private FolderService folderService;

	@AfterEach
	void afterEach() {
		fileMetadataRepository.deleteAllInBatch();
		folderMetadataRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("getFolderContents 메소드는")
	class getFolderContents {

		@Nested
		@DisplayName("CursorType 이 Folder 일 때")
		class withFolderCursor {

			@DisplayName("조회해야 하는 데이터 수가 조회 가능한 폴더 수보다 적으면 폴더 데이터만 전달하게 된다.")
			@Test
			void whenEnoughFolders() {
				// Given
				Long folderId = 1L;
				List<FolderMetadata> savedFolders = new ArrayList<>();
				for (int i = 1; i <= 10; i++) {
					savedFolders.add(folderMetadataRepository.save(createFolderMetadata(folderId, String.valueOf(i))));
				}

				// When
				FolderContentsDto result = folderService.getFolderContents(folderId, 0L, FOLDER, 5, CREATED_AT, ASC);

				// Then
				assertThat(result.folderMetadataList()).hasSize(5);
				assertThat(result.fileMetadataList()).isEmpty();
			}

			@DisplayName("조회해야 하는 데이터 수가 조회가능한 폴더 수보다 적으면 파일까지 조회한다.")
			@Test
			void whenNotEnoughFolders() {
				// Given
				Long folderId = 1L;
				List<FolderMetadata> savedFolders = new ArrayList<>();
				List<FileMetadata> savedFiles = new ArrayList<>();
				for (int i = 1; i <= 3; i++) {
					savedFolders.add(folderMetadataRepository.save(createFolderMetadata(folderId, String.valueOf(i))));
				}
				for (int i = 1; i <= 3; i++) {
					savedFiles.add(fileMetadataRepository.save(createFileMetadata(folderId, String.valueOf(i))));
				}

				// When
				FolderContentsDto result = folderService.getFolderContents(folderId, 0L, FOLDER, 5, CREATED_AT, ASC);

				// Then
				assertThat(result.folderMetadataList()).hasSize(3);
				assertThat(result.fileMetadataList()).hasSize(2);
			}
		}

		@Nested
		@DisplayName("CursorType 이 File 일 때")
		class withFileCursor {

			@DisplayName("FolderMetadataList 사이즈가 0이다.")
			@Test
			void whenFileCursor() {
				// Given
				Long folderId = 1L;
				List<FileMetadata> savedFiles = new ArrayList<>();
				for (int i = 1; i <= 5; i++) {
					savedFiles.add(fileMetadataRepository.save(createFileMetadata(folderId, String.valueOf(i))));
				}

				// When
				FolderContentsDto result = folderService.getFolderContents(folderId, 0L, FILE, 3, CREATED_AT, ASC);

				// Then
				assertThat(result.folderMetadataList()).isEmpty();
				assertThat(result.fileMetadataList()).hasSize(3);
			}

			@DisplayName("조회가능한 파일이 없으면 FileMetadataList 사이즈가 0이다.")
			@Test
			void whenNoContents() {
				// Given
				Long folderId = 1L;

				// When
				FolderContentsDto result = folderService.getFolderContents(folderId, 0L, FOLDER, 5, CREATED_AT, ASC);

				// Then
				assertThat(result.folderMetadataList()).isEmpty();
				assertThat(result.fileMetadataList()).isEmpty();
			}
		}
	}

	private FolderMetadata createFolderMetadata(Long parentFolderId, String folderName) {
		return FolderMetadata.builder()
			.rootId(1L)
			.ownerId(1L)
			.creatorId(1L)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.parentFolderId(parentFolderId)
			.uploadFolderName(folderName)
			.build();
	}

	private FileMetadata createFileMetadata(Long parentFolderId, String fileName) {
		return FileMetadata.builder()
			.rootId(1L)
			.creatorId(1L)
			.fileType("txt")
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.parentFolderId(parentFolderId)
			.size(1000L)
			.uploadFileName(fileName)
			.uuidFileName("uuid-" + fileName)
			.uploadStatus(UploadStatus.SUCCESS)
			.build();
	}

}
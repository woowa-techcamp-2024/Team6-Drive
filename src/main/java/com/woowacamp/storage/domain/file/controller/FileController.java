package com.woowacamp.storage.domain.file.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.file.dto.FileMoveDto;
import com.woowacamp.storage.domain.file.service.FileService;
import com.woowacamp.storage.domain.folder.service.FolderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

	private final FileService fileService;
	private final FolderService folderService;

	@PatchMapping("/{fileId}")
	public void moveFile(@PathVariable Long fileId, @RequestBody FileMoveDto dto) {
		fileService.getFileMetadataBy(fileId, dto.userId());
		folderService.checkFolderOwnedBy(dto.targetFolderId(), dto.userId());
		fileService.moveFile(fileId, dto);
	}
}

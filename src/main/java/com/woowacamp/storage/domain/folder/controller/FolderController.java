package com.woowacamp.storage.domain.folder.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.folder.dto.FolderContentsDto;
import com.woowacamp.storage.domain.folder.dto.GetFolderContentsRequestParams;
import com.woowacamp.storage.domain.folder.dto.request.CreateFolderReqDto;
import com.woowacamp.storage.domain.folder.dto.request.FolderMoveDto;
import com.woowacamp.storage.domain.folder.service.FolderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/folders")
public class FolderController {

	private final FolderService folderService;

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public void createFolder(@Valid @RequestBody CreateFolderReqDto req) {
		folderService.createFolder(req);
	}

	@GetMapping("/{folderId}")
	public FolderContentsDto getFolderContents(@PathVariable Long folderId,
		@Valid @ModelAttribute GetFolderContentsRequestParams request) {

		folderService.checkFolderOwnedBy(folderId, request.userId());

		return folderService.getFolderContents(folderId, request.cursorId(), request.cursorType(), request.limit(),
			request.sortBy(), request.sortDirection(), request.localDateTime(), request.size());
	}

	@PatchMapping("/{folderId}")
	public void moveFolder(@PathVariable("folderId") Long sourceFolderId, @RequestBody FolderMoveDto dto) {
		folderService.checkFolderOwnedBy(sourceFolderId, dto.userId());
		folderService.checkFolderOwnedBy(dto.targetFolderId(), dto.userId());
		folderService.moveFolder(sourceFolderId, dto);
	}

	@DeleteMapping("/{folderId}")
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable Long folderId, @RequestParam Long userId) {
		folderService.deleteFolder(folderId, userId);
	}

}

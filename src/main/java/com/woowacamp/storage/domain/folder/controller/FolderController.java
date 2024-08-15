package com.woowacamp.storage.domain.folder.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.folder.dto.FolderContentsDto;
import com.woowacamp.storage.domain.folder.dto.GetFolderContentsRequestParams;
import com.woowacamp.storage.domain.folder.service.FolderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/folders")
public class FolderController {
	private final FolderService folderService;

	@GetMapping("/{folderId}")
	public FolderContentsDto getFolderContents(@PathVariable Long folderId,
		@Valid @ModelAttribute GetFolderContentsRequestParams request) {

		folderService.checkFolderOwnedBy(folderId, request.userId());

		return folderService.getFolderContents(folderId, request.cursorId(), request.cursorType(), request.limit(),
			request.sortBy(), request.sortDirection(), request.localDateTime(), request.size());
	}
}

package com.woowacamp.storage.domain.folder.controller;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.folder.dto.CursorType;
import com.woowacamp.storage.domain.folder.dto.FolderContentsDto;
import com.woowacamp.storage.domain.folder.dto.FolderContentsSortField;
import com.woowacamp.storage.domain.folder.service.FolderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/folders")
public class FolderController {
	private final FolderService folderService;

	@GetMapping("/{folderId}")
	public FolderContentsDto getFolderContents(@PathVariable Long folderId, @RequestParam Long userId,
		@RequestParam Long cursorId, @RequestParam CursorType cursorType, @RequestParam(defaultValue = "100") int size,
		@RequestParam(required = false, defaultValue = "createdAt") FolderContentsSortField sortBy,
		@RequestParam(required = false, defaultValue = "DESC") Sort.Direction sortDirection) {

		folderService.checkFolderOwnedBy(folderId, userId);

		return folderService.getFolderContents(folderId, cursorId, cursorType, size, sortBy, sortDirection);
	}
}

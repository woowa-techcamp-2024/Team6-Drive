package com.woowacamp.storage.domain.folder.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.folder.dto.FolderContentsDto;
import com.woowacamp.storage.domain.folder.service.FolderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/folders")
public class FolderController {
	private final FolderService folderService;

	@GetMapping("/{folderId}")
	private FolderContentsDto getFolderContents(
		@PathVariable Long folderId,
		@RequestParam Long userId,
		@PageableDefault(size = 100, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) @RequestParam PageRequest pageRequest) {
		folderService.checkFolderOwnedBy(folderId, userId);
		return folderService.getFolderContents(folderId, pageRequest);
	}
}

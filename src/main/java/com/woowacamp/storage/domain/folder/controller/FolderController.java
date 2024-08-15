package com.woowacamp.storage.domain.folder.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.folder.dto.request.CreateFolderReqDto;
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
}

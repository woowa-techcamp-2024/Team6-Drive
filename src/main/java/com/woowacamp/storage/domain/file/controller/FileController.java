package com.woowacamp.storage.domain.file.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.file.dto.FileDataDto;
import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.service.FileServiceS3;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

	private static final String bucketName = "group-6-drive";
	private final FileServiceS3 fileServiceS3;

	@GetMapping("/download/{fileId}")
	ResponseEntity<InputStreamResource> download(@PathVariable Long fileId, @RequestParam("userId") Long userId) {

		FileMetadata fileMetadata = fileServiceS3.getFileMetadataBy(fileId, userId);
		FileDataDto fileDataDto = fileServiceS3.downloadByS3(fileId, bucketName, fileMetadata.getUuidFileName());
		HttpHeaders headers = new HttpHeaders();
		// HTTP 응답 헤더에 Content-Type 설정
		headers.add(HttpHeaders.CONTENT_TYPE, fileMetadata.getFileType());
		headers.add(HttpHeaders.CONTENT_DISPOSITION,
			"attachment; filename=" + fileDataDto.fileMetadataDto().uploadFileName());

		return ResponseEntity.ok()
			.headers(headers)
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.body(new InputStreamResource(fileDataDto.fileInputStream()));
	}

}

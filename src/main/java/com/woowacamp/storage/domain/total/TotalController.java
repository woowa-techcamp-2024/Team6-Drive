package com.woowacamp.storage.domain.total;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TotalController {

	private final FileMetadataRepository fileMetadataRepository;
	private final FolderMetadataRepository folderMetadataRepository;

	@GetMapping("/totals/{userId}")
	public Map<String, Object> getTotals(@PathVariable Long userId) {
		List<FileMetadata> files = fileMetadataRepository.findByOwnerId(userId);
		List<FolderMetadata> folders = folderMetadataRepository.findByOwnerId(userId);

		Map<String, Object> response = new HashMap<>();
		response.put("files", files);
		response.put("folders", folders);

		return response;
	}
}

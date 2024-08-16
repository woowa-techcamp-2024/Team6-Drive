package com.woowacamp.storage.domain.file.dto;

import java.util.Map;

import lombok.Getter;

public final class UploadContext {
	@Getter
	private final String boundary;
	@Getter
	private final String finalBoundary;
	@Getter
	private final Map<String, String> formFields;
	private boolean isFileRead;
	@Getter
	private FileMetadataDto fileMetadata;

	public UploadContext(
		String boundary,
		String finalBoundary,
		Map<String, String> formFields,
		boolean isFileRead
	) {
		this.boundary = boundary;
		this.finalBoundary = finalBoundary;
		this.formFields = formFields;
		this.isFileRead = isFileRead;
	}

	public boolean isFileRead() {
		return isFileRead;
	}

	public void updateIsFileRead() {
		this.isFileRead = true;
	}

	public void updateFileMetadata(FileMetadataDto fileMetadata) {
		this.fileMetadata = fileMetadata;
	}
}

package com.woowacamp.storage.domain.file.dto;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;

import lombok.Getter;

@Getter
public final class UploadContext {
	private final String boundary;
	private final String finalBoundary;
	private final Map<String, String> formFields;
	private boolean isFileRead;
	private FileMetadataDto fileMetadata;
	private String imageFormat;
	private PipedOutputStream pos;
	private PipedInputStream pis;
	private boolean startedCreatedThumbnail;
	private boolean abortedCreateThumbnail;

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
		this.imageFormat = null;
		this.pos = null;
		this.pis = null;
		this.startedCreatedThumbnail = false;
		this.abortedCreateThumbnail = false;
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

	public void updateImageFormat(String imageFormat) throws IOException {
		this.imageFormat = imageFormat;
		this.pos = new PipedOutputStream();
		this.pis = new PipedInputStream();

		pis.connect(pos);
	}

	public void updateStartedCreatedThumbnail() {
		this.startedCreatedThumbnail = true;
	}

	public void abortCreateThumbnail() {
		this.abortedCreateThumbnail = true;
	}
}

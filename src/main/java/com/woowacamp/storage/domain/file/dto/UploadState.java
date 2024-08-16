package com.woowacamp.storage.domain.file.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadState {
	@Setter
	private InitiateMultipartUploadResult initResponse;
	private Map<String, List<PartETag>> partETagsMap = new HashMap<>();
	private int partNumber = 0;
	private long fileSize = 0;
	private FileMetadataDto fileMetadataDto;

	public void reset() {
		initResponse = null;
		partETagsMap = new HashMap<>();
		partNumber = 0;
		fileSize = 0;
	}

	public void addPartNumber() {
		this.partNumber++;
	}

	public void addFileSize(int fileSize) {
		this.fileSize += fileSize;
	}

	public void initPartEtag(String fileName) {
		partETagsMap.put(fileName, new ArrayList<>());
	}
}

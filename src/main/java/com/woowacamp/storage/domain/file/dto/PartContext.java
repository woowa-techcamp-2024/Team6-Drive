package com.woowacamp.storage.domain.file.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartContext {

	@Setter
	private boolean isInHeader = true;
	private Map<String, String> headers = new HashMap<>();
	private String currentFieldName;
	private String currentFileName;
	private String currentContentType;
	private String uploadFileName;
	private int partCount;

	public void reset() {
		isInHeader = true;
		headers.clear();
		currentFieldName = null;
		currentFileName = null;
		currentContentType = null;
		partCount = 0;
	}

	public void plusPartCount() {
		this.partCount++;
	}
}

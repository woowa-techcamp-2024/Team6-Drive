package com.woowacamp.storage.domain.file.dto;

import java.io.InputStream;

public record FileDataDto(FileMetadataDto fileMetadataDto, InputStream fileInputStream) {
}

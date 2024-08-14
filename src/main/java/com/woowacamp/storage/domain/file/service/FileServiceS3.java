package com.woowacamp.storage.domain.file.service;

import static com.woowacamp.storage.global.error.ErrorCode.*;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.woowacamp.storage.domain.file.dto.FileDataDto;
import com.woowacamp.storage.domain.file.dto.FileMetadataDto;
import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileServiceS3 {

	private final FileMetadataRepository fileMetadataRepository;
	private final AmazonS3 s3Client;

	public FileDataDto downloadByS3(Long fileId, String bucketName, String uuidFileName) {
		FileMetadata fileMetadata = fileMetadataRepository.findById(fileId).orElseThrow(FILE_NOT_FOUND::baseException);
		S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucketName, uuidFileName));
		return new FileDataDto(FileMetadataDto.fromEntity(fileMetadata), s3Object.getObjectContent());
	}

	public FileMetadata getFileMetadataBy(Long fileId, Long userId) {
		FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
			.orElseThrow(ErrorCode.FILE_NOT_FOUND::baseException);

		if (!Objects.equals(fileMetadata.getCreatorId(), userId)) {
			throw ErrorCode.ACCESS_DENIED.baseException();
		}
		return fileMetadata;
	}
}

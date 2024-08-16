package com.woowacamp.storage.domain.file.service;

import static com.woowacamp.storage.global.error.ErrorCode.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

	private final AmazonS3 amazonS3;
	private final FileMetadataRepository fileMetadataRepository;
	@Value("${cloud.aws.credentials.bucketName}")
	private String BUCKET_NAME;

	// private String BUCKET_NAME
	@Transactional
	public void deleteFile(Long fileId, Long userId) {
		FileMetadata fileMetadata = fileMetadataRepository.findByIdAndOwnerId(fileId, userId)
			.orElseThrow(ACCESS_DENIED::baseException);

		fileMetadataRepository.delete(fileMetadata);

		try {
			amazonS3.deleteObject(BUCKET_NAME, fileMetadata.getUuidFileName());
		} catch (AmazonS3Exception e) {
			throw ErrorCode.FILE_DELETE_FAILED.baseException();
		}
	}
}

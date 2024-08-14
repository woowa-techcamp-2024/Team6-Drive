package com.woowacamp.storage.global.scheduler;

import static java.time.ZonedDateTime.*;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.woowacamp.storage.domain.file.repository.FileMetadataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 파일을 완전히 쓰지 못해서 임시로 저장된 S3 데이터를 관리하는 클래스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AbortedUploadDeleteScheduler {

	private final AmazonS3 amazonS3;
	private final FileMetadataRepository fileMetadataRepository;
	private final String BUCKET_NAME = "group-6-drive";

	/**
	 * 완전히 쓰지 못한 파일들의 리스트를 추출합니다.
	 * 이후 해당 리스트에서 6시간이 지난 데이터만 삭제합니다.
	 * 실시간으로 쓰고 있는 데이터도 listMultipartUploads를 통해서 읽히기 때문에 긴 시간(6시간)이 지난 데이터만 제거합니다.
	 */
	@Scheduled(fixedDelay = 1000 * 60)
	public void deleteAbortedUpload() {
		MultipartUploadListing listing = amazonS3.listMultipartUploads(new ListMultipartUploadsRequest(BUCKET_NAME));
		List<MultipartUpload> abortedUploads = listing.getMultipartUploads();
		abortedUploads.stream()
			.filter(abortedUpload -> {
				ZonedDateTime now = now(ZoneId.of("UTC"));
				ZonedDateTime uploadDate = ofInstant(abortedUpload.getInitiated().toInstant(), ZoneId.of("UTC"));
				return Duration.between(now.toInstant(), uploadDate.toInstant()).abs().compareTo(Duration.ofHours(6))
					   >= 0;
			}).forEach(
				abortedUpload -> {
					amazonS3.abortMultipartUpload(new AbortMultipartUploadRequest(BUCKET_NAME, abortedUpload.getKey(),
						abortedUpload.getUploadId()));
					fileMetadataRepository.deleteByUuidFileName(abortedUpload.getKey());
				});
	}
}
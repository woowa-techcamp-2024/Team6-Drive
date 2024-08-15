package com.woowacamp.storage.domain.file.service;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.woowacamp.storage.domain.file.dto.PartContext;
import com.woowacamp.storage.domain.file.util.CustomS3BlockingQueuePolicy;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileWriterThreadPool {
	@Value("${cloud.aws.credentials.bucketName}")
	private String BUCKET_NAME;

	private final Map<String, Integer> maxPartCountMap;
	private final Map<String, AtomicInteger> currentPartCountMap;
	private final AmazonS3 amazonS3;
	private final ExecutorService executorService;

	public FileWriterThreadPool(AmazonS3 amazonS3) {
		this.amazonS3 = amazonS3;
		this.maxPartCountMap = new HashMap<>();
		this.currentPartCountMap = new HashMap<>();
		this.executorService = new ThreadPoolExecutor(
			corePoolSize,
			maximumPoolSize,
			keepAliveTime,
			unit,
			workQueue,
			new CustomS3BlockingQueuePolicy()
		);
	}

	public void produce(InitiateMultipartUploadResult initResponse, String currentFileName, int partNumber,
		byte[] contentBuffer, int bufferLength, List<PartETag> partETags) {

		executorService.execute(() -> {
			log.info("partNumber: {}", partNumber);
			uploadPart(initResponse.getUploadId(), currentFileName, partNumber, contentBuffer, bufferLength, partETags);
			int currentConsumeCount = currentPartCountMap.get(currentFileName).incrementAndGet();
			Integer maxConsumeCount = maxPartCountMap.get(currentFileName);
			if (maxConsumeCount != null && currentConsumeCount >= maxConsumeCount) {
				completeFileUpload(initResponse.getUploadId(), currentFileName, partETags);
			}
		});
	}

	public void finishFileUpload(PartContext partContext) {
		maxPartCountMap.put(partContext.getUploadFileName(), partContext.getPartCount());
	}

	private void uploadPart(String uploadId, String key, int partNumber, byte[] data, int length,
		List<PartETag> partETags) {
		UploadPartRequest uploadRequest = new UploadPartRequest()
			.withBucketName(BUCKET_NAME)
			.withKey(key)
			.withUploadId(uploadId)
			.withPartNumber(partNumber)
			.withInputStream(new ByteArrayInputStream(data, 0, length))
			.withPartSize(length);
		UploadPartResult uploadResult = amazonS3.uploadPart(uploadRequest);
		partETags.add(uploadResult.getPartETag());
	}

	private void completeFileUpload(String uploadId, String currentFileName, List<PartETag> partETags) {
		if (maxPartCountMap.get(currentFileName) != partETags.size()) {
			amazonS3.abortMultipartUpload(new AbortMultipartUploadRequest(BUCKET_NAME, currentFileName, uploadId));
			return;
		}
		CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
			BUCKET_NAME, currentFileName, uploadId, partETags);
		amazonS3.completeMultipartUpload(completeRequest);
	}

	public void initializePartCount(String fileName) {
		currentPartCountMap.put(fileName, new AtomicInteger(0));
	}
}

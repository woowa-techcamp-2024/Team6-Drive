package com.woowacamp.storage.domain.file.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class SyncFileControllerV1 {

	private final AmazonS3 amazonS3;
	@Value("${cloud.aws.credentials.bucketName}")
	private String BUCKET_NAME = "group-6-drive";
	@Value("${file.reader.bufferSize}")
	private int BUFFER_SIZE;
	@Value("${file.reader.lineBufferMaxSize}")
	private int LINE_BUFFER_MAX_SIZE;
	@Value("${file.reader.chunkSize}")
	private int S3_CHUNK_SIZE;

	@PostMapping("/upload")
	public Map<String, Object> handleFileUpload(HttpServletRequest request) throws Exception {
		Map<String, Object> response = new HashMap<>();
		Map<String, String> formFields = new HashMap<>();
		List<String> uploadedFiles = new ArrayList<>();

		String contentType = request.getContentType();
		String boundary = "--" + extractBoundary(contentType);

		byte[] buffer = new byte[BUFFER_SIZE];
		ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
		int bytesRead;
		boolean isInHeader = true;
		Map<String, String> headers = new HashMap<>();
		String currentFieldName = null;
		String currentFileName = null;
		String currentContentType = null;
		ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();

		InitiateMultipartUploadResult initResponse = null;
		List<PartETag> partETags = null;
		int partNumber = 1;
		long fileSize = 0;

		try (InputStream inputStream = request.getInputStream()) {
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				for (int i = 0; i < bytesRead; i++) {
					byte b = buffer[i];
					lineBuffer.write(b);

					if (b == '\n') {
						String line = new String(lineBuffer.toByteArray()).trim();

						if (line.equals(boundary) || line.equals(boundary + "--")) {
							if (!isInHeader && currentFieldName != null) {
								if (currentFileName != null) {
									// Finish uploading file
									if (contentBuffer.size() > 0) {
										uploadPart(initResponse.getUploadId(), currentFileName, partNumber++,
											contentBuffer.toByteArray(), contentBuffer.size() - 2, partETags);
										fileSize += contentBuffer.size();
									}
									completeFileUpload(initResponse.getUploadId(), currentFileName, partETags,
										fileSize);
									uploadedFiles.add(currentFileName);
								} else {
									// Process form field
									formFields.put(currentFieldName, contentBuffer.toString().trim());
								}
								contentBuffer.reset();
							}
							isInHeader = true;
							headers.clear();
							currentFieldName = null;
							currentFileName = null;
							initResponse = null;
							partETags = null;
							partNumber = 1;
							fileSize = 0;
						} else if (isInHeader) {
							if (line.isEmpty()) {
								isInHeader = false;
								currentFieldName = extractFieldName(headers.get("Content-Disposition"));
								currentFileName = extractFileName(headers.get("Content-Disposition"));
								currentContentType = headers.get("Content-Type");
								if (currentFileName != null) {
									// Initialize file upload
									ObjectMetadata metadata = new ObjectMetadata();
									metadata.setContentType(currentContentType);
									InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
										BUCKET_NAME, currentFileName)
										.withObjectMetadata(metadata);
									initResponse = amazonS3.initiateMultipartUpload(initRequest);
									partETags = new ArrayList<>();
								}
							} else {
								int colonIndex = line.indexOf(':');
								if (colonIndex > 0) {
									String headerName = line.substring(0, colonIndex).trim();
									String headerValue = line.substring(colonIndex + 1).trim();
									headers.put(headerName, headerValue);
								}
							}
						} else {
							// Process content
							contentBuffer.write(lineBuffer.toByteArray());
							if (currentFileName != null && contentBuffer.size() >= S3_CHUNK_SIZE) {
								uploadPart(initResponse.getUploadId(), currentFileName, partNumber++,
									contentBuffer.toByteArray(), contentBuffer.size(), partETags);
								fileSize += contentBuffer.size();
								contentBuffer.reset();
							}
						}
						lineBuffer.reset();
					}

					// 파일 사이즈 계산해서 전체 용량이 넘지 않는지 확인 필요
				}

				// lineBuffer OOM 방지를 위한 메모리 체크
				if (lineBuffer.size() >= LINE_BUFFER_MAX_SIZE) {
					contentBuffer.write(lineBuffer.toByteArray());
					lineBuffer.reset();

					if (contentBuffer.size() >= S3_CHUNK_SIZE) {
						uploadPart(initResponse.getUploadId(), currentFileName, partNumber++,
							contentBuffer.toByteArray(),
							contentBuffer.size(), partETags);
						fileSize += contentBuffer.size();
						contentBuffer.reset();
					}
				}

			}
		}

		response.put("formFields", formFields);
		response.put("uploadedFiles", uploadedFiles);
		return response;
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

	private void completeFileUpload(String uploadId, String key, List<PartETag> partETags, long fileSize) {
		CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
			BUCKET_NAME, key, uploadId, partETags);
		amazonS3.completeMultipartUpload(completeRequest);

		System.out.println("File uploaded successfully. Key: " + key + ", Size: " + fileSize);
	}

	private String extractBoundary(String contentType) {
		for (String part : contentType.split(";")) {
			if (part.trim().startsWith("boundary=")) {
				return part.split("=", 2)[1].trim().replaceAll("^\"|\"$", "");
			}
		}
		return null;
	}

	private String extractFieldName(String contentDisposition) {
		return extractAttribute(contentDisposition, "name");
	}

	private String extractFileName(String contentDisposition) {
		return extractAttribute(contentDisposition, "filename");
	}

	private String extractAttribute(String source, String attribute) {
		for (String part : source.split(";")) {
			if (part.trim().startsWith(attribute + "=")) {
				return part.split("=", 2)[1].trim().replaceAll("^\"|\"$", "");
			}
		}
		return null;
	}

	@GetMapping("")
	public void downloadFile(@RequestParam String key) {
		try {
			S3Object object = amazonS3.getObject("group-6-drive", key);
			S3ObjectInputStream inputStream = object.getObjectContent();

			File file = new File("/Users/yoonjungjin/Desktop/IdeaProjects/Team6-Drive/" + LocalDateTime.now());
			if (!file.createNewFile()) {
				System.out.println("create new file");
			}
			com.amazonaws.util.IOUtils.copy(inputStream, new java.io.FileOutputStream(file));

			System.out.println("File downloaded successfully: " + file.getAbsolutePath());
		} catch (Exception e) {
			System.err.println("Error downloading file: " + e.getMessage());
		}
	}
}

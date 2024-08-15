// package com.woowacamp.storage.domain.file.controller;
//
// import java.io.ByteArrayOutputStream;
// import java.io.InputStream;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
//
// import com.amazonaws.services.s3.AmazonS3;
// import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
// import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
// import com.amazonaws.services.s3.model.ObjectMetadata;
// import com.amazonaws.services.s3.model.PartETag;
// import com.woowacamp.storage.domain.file.service.FileWriterThreadPool;
// import com.woowacamp.storage.domain.file.service.S3FileService;
//
// import jakarta.servlet.http.HttpServletRequest;
// import lombok.Getter;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
//
// @RestController
// @RequiredArgsConstructor
// @RequestMapping("/api/v1/files/async")
// @Slf4j
// public class AsyncFileControllerV1 {
//
// 	private final AmazonS3 amazonS3;
// 	private final FileWriterThreadPool fileWriterThreadPool;
//
// 	private static final String BUCKET_NAME = "group-6-drive";
// 	private static final int BUFFER_SIZE = 8 * 1024;
// 	private static final int LINE_BUFFER_MAX_SIZE = 1024 * 1024;
// 	private static final int S3_CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
// 	public static List<Thread> threads = new ArrayList<>();
// 	private final S3FileService s3FileService;
//
// 	@PostMapping("/upload")
// 	public Map<String, Object> handleFileUpload(HttpServletRequest request) throws Exception {
// 		Map<String, Object> response = new HashMap<>();
// 		Map<String, String> formFields = new HashMap<>();
// 		List<String> uploadedFiles = new ArrayList<>();
//
// 		String boundary = "--" + extractBoundary(request.getContentType());
// 		String finalBoundary = boundary + "--";
//
// 		try (InputStream inputStream = request.getInputStream()) {
// 			// UploadContext 초기화
// 			UploadContext context = new UploadContext(boundary, finalBoundary, formFields, uploadedFiles);
// 			processMultipartData(inputStream, context);
// 		}
//
// 		response.put("formFields", formFields);
// 		response.put("uploadedFiles", uploadedFiles);
// 		return response;
// 	}
//
// 	private void processMultipartData(InputStream inputStream, UploadContext context) throws Exception {
// 		byte[] buffer = new byte[BUFFER_SIZE];
// 		ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
// 		ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
// 		PartContext partContext = new PartContext();
// 		UploadState state = new UploadState();
//
// 		int bytesRead;
// 		while ((bytesRead = inputStream.read(buffer)) != -1) {
// 			processBuffer(buffer, bytesRead, lineBuffer, contentBuffer, context, partContext, state);
// 		}
// 	}
//
// 	/**
// 	 * line 단위로 buffer를 읽어들임
// 	 */
// 	private void processBuffer(byte[] buffer, int bytesRead, ByteArrayOutputStream lineBuffer,
// 		ByteArrayOutputStream contentBuffer, UploadContext context,
// 		PartContext partContext, UploadState state) throws Exception {
// 		for (int i = 0; i < bytesRead; i++) {
// 			byte b = buffer[i];
// 			lineBuffer.write(b);
//
// 			if (b != '\n') {
// 				continue;
// 			}
// 			String line = lineBuffer.toString().trim();
//
// 			if (line.equals(context.boundary) || line.equals(context.finalBoundary)) {
// 				// boundary 한 줄을 읽은 경우
// 				processEndOfPart(contentBuffer, context, partContext, state);
// 				resetState(partContext, state);
// 				// boundary가 끝난 뒤에 데이터가 남아 있으면 항상 헤더부터 시작
// 				partContext.isInHeader = true;
// 			} else if (partContext.isInHeader) {
// 				// boundary 읽은 이후
// 				processHeader(line, partContext);
// 				if (!partContext.isInHeader && partContext.currentFileName != null) {
// 					state.initResponse = initializeFileUpload(partContext.currentFileName,
// 						partContext.currentContentType);
// 					state.partETagsMap.put(partContext.currentFileName, new ArrayList<>());
// 				}
// 			} else {
// 				processContent(contentBuffer, lineBuffer, partContext, state);
// 			}
// 			lineBuffer.reset();
// 		}
//
// 		checkLineBufferSize(lineBuffer, contentBuffer, partContext, state);
// 	}
//
// 	/**
// 	 * file인 경우, 남은 buffer의 내용을 upload하고
// 	 * field인 경우, response에 추가
// 	 */
// 	private void processEndOfPart(ByteArrayOutputStream contentBuffer, UploadContext context,
// 		PartContext partContext, UploadState state) {
// 		if (partContext.isInHeader || partContext.currentFieldName == null) {
// 			return;
// 		}
// 		if (partContext.currentFileName != null) {
// 			finishFileUpload(contentBuffer, state, partContext);
// 			context.uploadedFiles.add(partContext.currentFileName);
// 		} else {
// 			context.formFields.put(partContext.currentFieldName, contentBuffer.toString().trim());
// 		}
// 		contentBuffer.reset();
// 	}
//
// 	/**
// 	 * 다음 part에 대한 요청을 읽어들이기 위해 context, state 초기화
// 	 */
// 	private void resetState(PartContext partContext, UploadState state) {
// 		partContext.reset();
// 		state.reset();
// 	}
//
// 	/**
// 	 *
// 	 */
// 	private void processHeader(String line, PartContext partContext) {
// 		if (line.isEmpty()) {
// 			partContext.currentFieldName = extractFieldName(partContext.headers.get("Content-Disposition"));
// 			partContext.currentFileName = extractFileName(partContext.headers.get("Content-Disposition"));
// 			partContext.currentContentType = partContext.headers.get("Content-Type");
// 			partContext.isInHeader = false;
// 		} else {
// 			int colonIndex = line.indexOf(':');
// 			if (colonIndex > 0) {
// 				String headerName = line.substring(0, colonIndex).trim();
// 				String headerValue = line.substring(colonIndex + 1).trim();
// 				partContext.headers.put(headerName, headerValue);
// 			}
// 		}
// 	}
//
// 	/**
// 	 * header를 읽은 후, file type이라면
// 	 * S3에 part upload를 알리는 initiate request
// 	 */
// 	private InitiateMultipartUploadResult initializeFileUpload(String fileName, String contentType) {
// 		fileWriterThreadPool.initializePartCount(fileName);
// 		ObjectMetadata metadata = new ObjectMetadata();
// 		metadata.setContentType(contentType);
// 		InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(BUCKET_NAME, fileName)
// 			.withObjectMetadata(metadata);
// 		return amazonS3.initiateMultipartUpload(initRequest);
// 	}
//
// 	/**
// 	 * contentBuffer가 5MB 이상 찼을 경우, part upload
// 	 */
// 	private void processContent(ByteArrayOutputStream contentBuffer, ByteArrayOutputStream lineBuffer,
// 		PartContext partContext, UploadState state) throws Exception {
// 		contentBuffer.write(lineBuffer.toByteArray());
// 		if (partContext.currentFileName != null && contentBuffer.size() >= S3_CHUNK_SIZE) {
// 			uploadChunk(contentBuffer, state, partContext);
// 		}
// 	}
//
// 	/**
// 	 * s3 part upload
// 	 */
// 	private void uploadChunk(ByteArrayOutputStream contentBuffer, UploadState state, PartContext partContext) {
// 		partContext.plusPartCount();
// 		state.partNumber++;
// 		state.fileSize += contentBuffer.size();
// 		fileWriterThreadPool.produce(state.initResponse, partContext.currentFileName, state.partNumber,
// 			contentBuffer.toByteArray(), contentBuffer.size(),
// 			state.partETagsMap.get(partContext.currentFileName));
// 		contentBuffer.reset();
// 	}
//
// 	/**
// 	 * lineBuffer가 일정 수준 차면 contentBuffer로 flush
// 	 * contentBuffer가 5MB 이상 차면, s3로 part upload
// 	 */
// 	private void checkLineBufferSize(ByteArrayOutputStream lineBuffer, ByteArrayOutputStream contentBuffer,
// 		PartContext partContext, UploadState state) throws Exception {
// 		if (lineBuffer.size() >= LINE_BUFFER_MAX_SIZE) {
// 			contentBuffer.write(lineBuffer.toByteArray());
// 			lineBuffer.reset();
//
// 			if (contentBuffer.size() >= S3_CHUNK_SIZE) {
// 				uploadChunk(contentBuffer, state, partContext);
// 			}
// 		}
// 	}
//
// 	/**
// 	 * part upload가 모두 끝난 이후, 최종 finish upload 요청
// 	 * 마지막 업로드인데, 버퍼에 남은 데이터가 있는 경우(5MB 이하)
// 	 */
// 	private void finishFileUpload(ByteArrayOutputStream contentBuffer, UploadState state, PartContext partContext) {
// 		uploadLeftOver(contentBuffer, state, partContext);
// 		fileWriterThreadPool.finishFileUpload(partContext);
// 	}
//
// 	/**
// 	 * 마지막 part의 contentBuffer가 남아있는 경우
// 	 * 5MB 이하의 파일을 최종적으로 part upload
// 	 */
// 	private void uploadLeftOver(ByteArrayOutputStream contentBuffer, UploadState state, PartContext partContext) {
// 		if (contentBuffer.size() == 0) {
//
// 			return;
// 		}
// 		partContext.plusPartCount();
// 		state.partNumber++;
// 		state.fileSize += contentBuffer.size() - 2;
// 		log.info("[Last Upload Ended] file total size = {}",state.fileSize);
// 		fileWriterThreadPool.produce(state.initResponse, partContext.currentFileName, state.partNumber,
// 			contentBuffer.toByteArray(), contentBuffer.size() - 2,
// 			state.partETagsMap.get(partContext.currentFileName));
// 	}
//
// 	/**
// 	 * boundary 추출
// 	 */
// 	private String extractBoundary(String contentType) {
// 		for (String part : contentType.split(";")) {
// 			if (part.trim().startsWith("boundary=")) {
// 				return part.split("=", 2)[1].trim().replaceAll("^\"|\"$", "");
// 			}
// 		}
// 		return null;
// 	}
//
// 	private String extractFieldName(String contentDisposition) {
// 		return extractAttribute(contentDisposition, "name");
// 	}
//
// 	private String extractFileName(String contentDisposition) {
// 		return extractAttribute(contentDisposition, "filename");
// 	}
//
// 	private String extractAttribute(String source, String attribute) {
// 		for (String part : source.split(";")) {
// 			if (part.trim().startsWith(attribute + "=")) {
// 				return part.split("=", 2)[1].trim().replaceAll("^\"|\"$", "");
// 			}
// 		}
// 		return null;
// 	}
//
// 	private record UploadContext(
// 		String boundary,
// 		String finalBoundary,
// 		Map<String, String> formFields,
// 		List<String> uploadedFiles
// 	) {
// 	}
//
// 	@Getter
// 	public static class PartContext {
// 		private boolean isInHeader = true;
// 		private Map<String, String> headers = new HashMap<>();
// 		private String currentFieldName;
// 		private String currentFileName;
// 		private String currentContentType;
// 		private int partCount;
//
// 		public void reset() {
// 			isInHeader = true;
// 			headers.clear();
// 			currentFieldName = null;
// 			currentFileName = null;
// 			currentContentType = null;
// 			partCount = 0;
// 		}
//
// 		public void plusPartCount() {
// 			this.partCount++;
// 		}
// 	}
//
// 	@Getter
// 	private static class UploadState {
// 		private InitiateMultipartUploadResult initResponse;
// 		private Map<String, List<PartETag>> partETagsMap = new HashMap<>();
// 		private int partNumber = 0;
// 		private long fileSize = 0;
//
// 		public void reset() {
// 			initResponse = null;
// 			partETagsMap = new HashMap<>();
// 			partNumber = 0;
// 			fileSize = 0;
// 		}
// 	}
// }

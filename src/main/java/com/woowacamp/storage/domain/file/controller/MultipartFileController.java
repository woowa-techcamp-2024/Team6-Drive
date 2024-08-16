package com.woowacamp.storage.domain.file.controller;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.woowacamp.storage.domain.file.dto.FileDataDto;
import com.woowacamp.storage.domain.file.dto.FileMetadataDto;
import com.woowacamp.storage.domain.file.dto.FormMetadataDto;
import com.woowacamp.storage.domain.file.dto.PartContext;
import com.woowacamp.storage.domain.file.dto.UploadContext;
import com.woowacamp.storage.domain.file.dto.UploadState;
import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.service.FileWriterThreadPool;
import com.woowacamp.storage.domain.file.service.S3FileService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
@Slf4j
public class MultipartFileController {

	private final AmazonS3 amazonS3;
	private final S3FileService s3FileService;
	private final FileWriterThreadPool fileWriterThreadPool;

	@Value("${cloud.aws.credentials.bucketName}")
	private String BUCKET_NAME;
	@Value("${file.reader.bufferSize}")
	private int BUFFER_SIZE;
	@Value("${file.reader.lineBufferMaxSize}")
	private int LINE_BUFFER_MAX_SIZE;
	@Value("${file.reader.chunkSize}")
	private int S3_CHUNK_SIZE;

	/**
	 *
	 * MultipartFile은 임시 저장을 해서 직접 request를 통해 multipart/form-data를 파싱했습니다.
	 * 파싱을 하고 S3에 이미지를 업로드합니다.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public void handleFileUpload(HttpServletRequest request) throws Exception {
		String boundary = "--" + extractBoundary(request.getContentType());
		String finalBoundary = boundary + "--";

		try (InputStream inputStream = request.getInputStream()) {
			UploadContext context = new UploadContext(boundary, finalBoundary, new HashMap<>(), false);
			processMultipartData(inputStream, context);
		}
	}

	/**
	 * 클라이언트 요청을 buffer 단위로 읽어서 processBuffer 메소드를 호출합니다.
	 * processBuffer 메소드에서 헤더 파싱을 하고 boundary 체크를 하여 각 파트를 구분합니다.
	 */
	private void processMultipartData(InputStream inputStream, UploadContext context) throws Exception {
		byte[] buffer = new byte[BUFFER_SIZE];
		ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
		ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
		PartContext partContext = new PartContext();
		UploadState state = new UploadState();

		int bytesRead;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			if (processBuffer(buffer, bytesRead, lineBuffer, contentBuffer, context, partContext, state)) {
				break;
			}
		}
	}

	/**
	 * line 단위로 buffer를 읽어들입니다.
	 * boundary 기준으로 헤더를 먼저 읽고 나머지는 processContent로 content 처리를 합니다.
	 * 헤더 데이터를 읽은 후, 1차로 사용자 정보, 파일 이름 등의 메타데이터를 저장합니다.
	 *
	 * @return - true인 경우 API 명세에 따라 추가 데이터는 읽지 않는다.
	 */
	private boolean processBuffer(byte[] buffer, int bytesRead, ByteArrayOutputStream lineBuffer,
		ByteArrayOutputStream contentBuffer, UploadContext context, PartContext partContext, UploadState state) throws
		Exception {
		for (int i = 0; i < bytesRead; i++) {
			byte b = buffer[i];
			lineBuffer.write(b);

			if (b != '\n') {
				continue;
			}
			String line = lineBuffer.toString().trim();

			if (line.equals(context.getBoundary()) || line.equals(context.getFinalBoundary())) {
				// boundary 한 줄을 읽은 경우
				processEndOfPart(contentBuffer, context, partContext, state);
				if (context.isFileRead()) {
					s3FileService.finalizeMetadata(context.getFileMetadata(), state.getFileSize());
					return true;
				}
				resetState(partContext, state);
				// boundary가 끝난 뒤에 데이터가 남아 있으면 항상 헤더부터 시작
				partContext.setInHeader(true);
			} else if (partContext.isInHeader()) {
				// boundary 읽은 이후
				processHeader(line, partContext);
				if (!partContext.isInHeader() && partContext.getCurrentFileName() != null) {
					FileMetadataDto fileMetadataDto = s3FileService.createInitialMetadata(
						FormMetadataDto.of(context.getFormFields()), partContext);
					context.updateFileMetadata(fileMetadataDto);
					context.updateIsFileRead();
					partContext.setUploadFileName(fileMetadataDto.uuid());
					state.setInitResponse(
						initializeFileUpload(partContext.getUploadFileName(), partContext.getCurrentContentType()));
					state.initPartEtag(partContext.getUploadFileName());
					state.setFileMetadataDto(fileMetadataDto);
				}
			} else {
				processContent(contentBuffer, lineBuffer, partContext, state);
			}
			lineBuffer.reset();
		}

		checkLineBufferSize(lineBuffer, contentBuffer, partContext, state);
		return false;
	}

	/**
	 * file인 경우, 남은 buffer의 내용을 upload 하고 field인 경우, response에 추가합니다.
	 */
	private void processEndOfPart(ByteArrayOutputStream contentBuffer, UploadContext context, PartContext partContext,
		UploadState state) {
		if (partContext.isInHeader() || partContext.getCurrentFieldName() == null) {
			return;
		}
		if (partContext.getCurrentFileName() != null) {
			finishFileUpload(contentBuffer, state, partContext);
			s3FileService.checkMetadata(state);
		} else {
			context.getFormFields().put(partContext.getCurrentFieldName(), contentBuffer.toString().trim());
		}
		contentBuffer.reset();
	}

	/**
	 * 다음 part에 대한 요청을 읽어들이기 위해 context, state 초기화
	 */
	private void resetState(PartContext partContext, UploadState state) {
		partContext.reset();
		state.reset();
	}

	/**
	 * 헤더 데이터를 파싱합니다.
	 */
	private void processHeader(String line, PartContext partContext) {
		if (line.isEmpty()) {
			partContext.setCurrentFieldName(extractFieldName(partContext.getHeaders().get("Content-Disposition")));
			partContext.setCurrentFileName(extractFileName(partContext.getHeaders().get("Content-Disposition")));
			partContext.setCurrentContentType(partContext.getHeaders().get("Content-Type"));
			partContext.setInHeader(false);
		} else {
			int colonIndex = line.indexOf(':');
			if (colonIndex > 0) {
				String headerName = line.substring(0, colonIndex).trim();
				String headerValue = line.substring(colonIndex + 1).trim();
				partContext.getHeaders().put(headerName, headerValue);
			}
		}
	}

	/**
	 * header를 읽은 후, file type이라면 S3에 part upload를 알려주는 initiate request 입니다.
	 */
	private InitiateMultipartUploadResult initializeFileUpload(String fileName, String contentType) {
		fileWriterThreadPool.initializePartCount(fileName);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType(contentType);
		InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(BUCKET_NAME,
			fileName).withObjectMetadata(metadata);
		return amazonS3.initiateMultipartUpload(initRequest);
	}

	/**
	 * contentBuffer가 5MB 이상 찼을 경우, part upload를 합니다.
	 */
	private void processContent(ByteArrayOutputStream contentBuffer, ByteArrayOutputStream lineBuffer,
		PartContext partContext, UploadState state) throws Exception {
		contentBuffer.write(lineBuffer.toByteArray());
		if (partContext.getCurrentFileName() != null && contentBuffer.size() >= S3_CHUNK_SIZE) {
			uploadChunk(contentBuffer, state, partContext);
		}
	}

	/**
	 * 실제로 파일 바이너리 데이터 쓰기 작업을 요청하는 메소드입니다.
	 * 별도의 쓰기 작업 스레드 풀에 요청을 넘겨줍니다.
	 */
	private void uploadChunk(ByteArrayOutputStream contentBuffer, UploadState state, PartContext partContext) {
		partContext.plusPartCount();
		state.addPartNumber();
		state.addFileSize(contentBuffer.size());
		fileWriterThreadPool.produce(state.getInitResponse(), partContext.getUploadFileName(), state.getPartNumber(),
			contentBuffer.toByteArray(), contentBuffer.size(),
			state.getPartETagsMap().get(partContext.getUploadFileName()));
		contentBuffer.reset();
	}

	/**
	 * OOM 방지를 위해 lineBuffer가 일정 수준 이상에 도달하면 contentBuffer로 flush 합니다.
	 * contentBuffer가 5MB 이상 차면, s3로 part upload를 합니다.
	 */
	private void checkLineBufferSize(ByteArrayOutputStream lineBuffer, ByteArrayOutputStream contentBuffer,
		PartContext partContext, UploadState state) throws Exception {
		if (lineBuffer.size() >= LINE_BUFFER_MAX_SIZE) {
			contentBuffer.write(lineBuffer.toByteArray());
			lineBuffer.reset();

			if (contentBuffer.size() >= S3_CHUNK_SIZE) {
				uploadChunk(contentBuffer, state, partContext);
			}
		}
	}

	/**
	 * part upload가 모두 끝난 이후, 최종 finish upload 요청을 합니다.
	 * 버퍼에 보내지 못한 데이터가 존재할 수 있으니 확인을 하고 쓰기 작업 스레드 풀에 작업이 끝났음을 알립니다.
	 */
	private void finishFileUpload(ByteArrayOutputStream contentBuffer, UploadState state, PartContext partContext) {
		uploadLeftOver(contentBuffer, state, partContext);
		fileWriterThreadPool.finishFileUpload(partContext);
	}

	/**
	 * 마지막 part의 contentBuffer가 남아있는 경우, 5MB 이하의 파일을 최종적으로 쓰기 작업 스레드 풀에 요청합니다.
	 */
	private void uploadLeftOver(ByteArrayOutputStream contentBuffer, UploadState state, PartContext partContext) {
		if (contentBuffer.size() == 0) {
			return;
		}
		partContext.plusPartCount();
		state.addPartNumber();
		state.addFileSize(contentBuffer.size() - 2);
		log.info("[Last Upload Ended] file total size = {}", state.getFileSize());
		fileWriterThreadPool.produce(state.getInitResponse(), partContext.getUploadFileName(), state.getPartNumber(),
			contentBuffer.toByteArray(), contentBuffer.size() - 2,
			state.getPartETagsMap().get(partContext.getUploadFileName()));
	}

	/**
	 * boundary를 추출합니다.
	 */
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

	@GetMapping("/download/{fileId}")
	ResponseEntity<InputStreamResource> download(@PathVariable Long fileId, @RequestParam("userId") Long userId) {

		FileMetadata fileMetadata = s3FileService.getFileMetadataBy(fileId, userId);
		FileDataDto fileDataDto = s3FileService.downloadByS3(fileId, BUCKET_NAME, fileMetadata.getUuidFileName());
		HttpHeaders headers = new HttpHeaders();
		// HTTP 응답 헤더에 Content-Type 설정
		headers.add(HttpHeaders.CONTENT_TYPE, fileMetadata.getFileType());
		headers.add(HttpHeaders.CONTENT_DISPOSITION,
			"attachment; filename=" + fileDataDto.fileMetadataDto().uploadFileName());

		return ResponseEntity.ok()
			.headers(headers)
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.body(new InputStreamResource(fileDataDto.fileInputStream()));
	}

	@GetMapping("/123")
	public void asd() {
		System.out.println(BUCKET_NAME);
	}
}

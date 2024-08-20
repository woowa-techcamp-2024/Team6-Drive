package com.woowacamp.storage.domain.shredlink.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.repository.FileMetadataJpaRepository;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.repository.FolderMetadataRepository;
import com.woowacamp.storage.domain.shredlink.dto.request.MakeSharedLinkRequestDto;
import com.woowacamp.storage.domain.shredlink.dto.response.SharedLinkResponseDto;
import com.woowacamp.storage.domain.shredlink.entity.SharedLink;
import com.woowacamp.storage.domain.shredlink.repository.SharedLinkRepository;
import com.woowacamp.storage.global.constant.CommonConstant;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SharedLinkService {
	@Value("${share.server-domain}")
	private String SERVER_DOMAIN;

	private final SharedLinkRepository sharedLinkRepository;
	private final FolderMetadataRepository folderMetadataRepository;
	private final FileMetadataJpaRepository fileMetadataJpaRepository;

	/**
	 * 공유 링크 생성 메소드
	 * 공유 링크와 토큰 값은 UUID를 사용했습니다.
	 */
	@Transactional
	public SharedLinkResponseDto createSharedLink(MakeSharedLinkRequestDto requestDto) {
		if (requestDto.isFile()) { // file인 경우
			FileMetadata fileMetadata = fileMetadataJpaRepository.findById(requestDto.targetId())
				.orElseThrow(ErrorCode.FILE_NOT_FOUND::baseException);

			if (!isOwner(fileMetadata.getOwnerId(), requestDto.userId())) {
				throw ErrorCode.ACCESS_DENIED.baseException();
			}
		} else { // folder인 경우
			FolderMetadata folderMetadata = folderMetadataRepository.findById(requestDto.targetId())
				.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);

			if (!isOwner(folderMetadata.getOwnerId(), requestDto.userId())) {
				throw ErrorCode.ACCESS_DENIED.baseException();
			}
		}

		LocalDateTime createTime = LocalDateTime.now();
		LocalDateTime expiredTime = createTime.plusHours(CommonConstant.SHARED_LINK_VALID_TIME);

		String sharedUrl = createSharedLink();
		String sharedToken = createToken();
		SharedLink sharedLink = SharedLink.builder()
			.createdAt(createTime)
			.sharedLinkUrl(sharedUrl)
			.sharedUserId(requestDto.userId())
			.sharedToken(sharedToken)
			.expiredAt(expiredTime)
			.isFile(requestDto.isFile())
			.targetId(requestDto.targetId())
			.build();

		try {
			sharedLinkRepository.saveAndFlush(sharedLink);
		} catch (DataIntegrityViolationException e) {
			throw ErrorCode.DUPLICATED_SHARED_LINK.baseException();
		}

		return new SharedLinkResponseDto(sharedUrl);
	}

	private boolean isOwner(long ownerId, long userId) {
		return ownerId == userId;
	}

	private String createSharedLink() {
		return SERVER_DOMAIN + CommonConstant.SHARED_URI + UUID.randomUUID();
	}

	private String createToken() {
		return UUID.randomUUID().toString();
	}
}

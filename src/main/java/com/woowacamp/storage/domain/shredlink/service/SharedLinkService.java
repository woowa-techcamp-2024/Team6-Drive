package com.woowacamp.storage.domain.shredlink.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
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
import com.woowacamp.storage.domain.shredlink.entity.SharedLinkFactory;
import com.woowacamp.storage.domain.shredlink.event.ShareEvent;
import com.woowacamp.storage.domain.shredlink.repository.SharedLinkRepository;
import com.woowacamp.storage.global.constant.CommonConstant;
import com.woowacamp.storage.global.constant.PermissionType;
import com.woowacamp.storage.global.error.ErrorCode;
import com.woowacamp.storage.global.util.UrlUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SharedLinkService {
	private final SharedLinkRepository sharedLinkRepository;
	private final FolderMetadataRepository folderMetadataRepository;
	private final FileMetadataJpaRepository fileMetadataJpaRepository;
	private final ApplicationEventPublisher applicationEventPublisher;

	/**
	 * 공유 링크 생성 메소드
	 *공유 대상 폴더/파일(폴더라면 하위 폴더 및 파일까지)의   공유 상태를 업데이트 하고 공유 링크를 반환합니다.
	 */
	@Transactional
	public SharedLinkResponseDto createShareLink(MakeSharedLinkRequestDto requestDto) {
		validateRequest(requestDto);

		// 기존에 발급된 링크가 있다면 반환
		Optional<SharedLink> existingSharedLink = getExistingSharedLink(requestDto.isFile(), requestDto.targetId(),
			requestDto.getPermissionType());
		if (existingSharedLink.isPresent()) {
			return new SharedLinkResponseDto(createSharedLinkUrl(existingSharedLink.get().getSharedId()));
		}

		SharedLink sharedLink = createSharedLink(requestDto);
		try {
			sharedLinkRepository.saveAndFlush(sharedLink);
		} catch (DataIntegrityViolationException e) {
			throw ErrorCode.DUPLICATED_SHARED_LINK.baseException();
		}

		// 비동기로 폴더 및 파일의 공유 상태 업데이트
		applicationEventPublisher.publishEvent(new ShareEvent(this, sharedLink));

		return new SharedLinkResponseDto(createSharedLinkUrl(sharedLink.getSharedId()));
	}

	private void validateRequest(MakeSharedLinkRequestDto requestDto) {
		if (requestDto.isFile()) { // file인 경우
			FileMetadata fileMetadata = fileMetadataJpaRepository.findById(requestDto.targetId())
				.orElseThrow(ErrorCode.FILE_NOT_FOUND::baseException);
			if (!Objects.equals(fileMetadata.getOwnerId(), requestDto.userId())) {
				throw ErrorCode.ACCESS_DENIED.baseException();
			}
		} else { // folder인 경우
			FolderMetadata folderMetadata = folderMetadataRepository.findById(requestDto.targetId())
				.orElseThrow(ErrorCode.FOLDER_NOT_FOUND::baseException);
			if (!Objects.equals(folderMetadata.getOwnerId(), requestDto.userId())) {
				throw ErrorCode.ACCESS_DENIED.baseException();
			}
		}
	}

	private Optional<SharedLink> getExistingSharedLink(boolean isFile, long targetId, PermissionType permissionType) {
		List<SharedLink> existingLinks = sharedLinkRepository.findByIsFileAndTargetId(isFile, targetId);
		for (SharedLink link : existingLinks) {
			if (!link.isExpired() && Objects.equals(permissionType, link.getPermissionType())) {
				return Optional.of(link);
			}
		}
		return Optional.empty();
	}

	private SharedLink createSharedLink(MakeSharedLinkRequestDto requestDto) {
		String sharedId = UUID.randomUUID().toString();
		String redirectUrl = createRedirectUrl(requestDto.isFile(), requestDto.targetId());
		return SharedLinkFactory.createSharedLink(requestDto, sharedId, redirectUrl);
	}

	private String createRedirectUrl(boolean isFile, long targetId) {
		String template = isFile ? CommonConstant.FILE_READ_URI_TEMPLATE : CommonConstant.FOLDER_READ_URI_TEMPLATE;
		return UrlUtil.getAbsoluteUrl(String.format(template, targetId));
	}

	private String createSharedLinkUrl(String sharedId) {
		return UrlUtil.getAbsoluteUrl(CommonConstant.SHARED_LINK_URI + sharedId);
	}

	/** 공유 대상을 조회하는 url을  반환합니다.
	 **/
	public String getRedirectUrl(String sharedId) {
		SharedLink sharedLink = sharedLinkRepository.findBySharedId(sharedId)
			.orElseThrow(ErrorCode.SHARED_LINK_NOT_FOUND::baseException);
		if (sharedLink.isExpired()) {
			throw ErrorCode.EXPIRED_SHARED_LINK.baseException();
		}
		return sharedLink.getRedirectUrl();
	}
}

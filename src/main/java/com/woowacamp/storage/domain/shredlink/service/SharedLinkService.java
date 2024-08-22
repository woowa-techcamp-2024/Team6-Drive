package com.woowacamp.storage.domain.shredlink.service;

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
import com.woowacamp.storage.domain.shredlink.entity.SharedLinkFactory;
import com.woowacamp.storage.domain.shredlink.repository.SharedLinkRepository;
import com.woowacamp.storage.global.constant.CommonConstant;
import com.woowacamp.storage.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SharedLinkService {

	private final SharedLinkRepository sharedLinkRepository;
	private final FolderMetadataRepository folderMetadataRepository;
	private final FileMetadataJpaRepository fileMetadataJpaRepository;

	/**
	 * 공유 링크 생성 메소드
	 *공유 대상 폴더/파일(폴더라면 하위 폴더 및 파일까지)의   공유 상태를 업데이트 하고 공유 링크를 반환합니다.
	 */
	@Transactional
	public SharedLinkResponseDto createShareLink(MakeSharedLinkRequestDto requestDto) {
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

		String sharedUrl = createSharedLink();
		String sharedToken = createToken();
		SharedLink sharedLink = SharedLinkFactory.createSharedLink(requestDto, sharedUrl, sharedToken);

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

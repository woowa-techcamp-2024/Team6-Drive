package com.woowacamp.storage.domain.shredlink.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.woowacamp.storage.domain.shredlink.dto.request.MakeSharedLinkRequestDto;
import com.woowacamp.storage.global.constant.CommonConstant;

public class SharedLinkFactory {
	public static SharedLink createSharedLink(MakeSharedLinkRequestDto requestDto,
		String sharedToken, String sharedLink, String sharedId) {
		LocalDateTime createTime = LocalDateTime.now();
		LocalDateTime expiredTime = createTime.plus(CommonConstant.SHARED_LINK_VALID_TIME, ChronoUnit.SECONDS);
		String sharedUrl = CommonConstant.SHARED_URI + sharedId;
		return SharedLink.builder()
			.createdAt(createTime)
			.sharedLinkUrl(sharedUrl)
			.sharedUserId(requestDto.userId())
			.sharedToken(sharedToken)
			.sharedId(sharedId)
			.expiredAt(expiredTime)
			.isFile(requestDto.isFile())
			.targetId(requestDto.targetId())
			.permissionType(requestDto.getPermissionType())
			.build();
	}
}

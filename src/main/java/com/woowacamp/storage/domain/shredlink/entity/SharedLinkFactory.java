package com.woowacamp.storage.domain.shredlink.entity;

import java.time.LocalDateTime;

import com.woowacamp.storage.domain.shredlink.dto.request.MakeSharedLinkRequestDto;
import com.woowacamp.storage.global.constant.CommonConstant;

public class SharedLinkFactory {

	public static SharedLink createSharedLink(MakeSharedLinkRequestDto requestDto, String sharedId,
		String redirectUrl) {
		LocalDateTime createTime = LocalDateTime.now();
		LocalDateTime expiredTime = createTime.plusSeconds(CommonConstant.SHARED_LINK_VALID_TIME);
		return SharedLink.builder()
			.createdAt(createTime)
			.redirectUrl(redirectUrl)
			.sharedUserId(requestDto.userId())
			.sharedId(sharedId)
			.expiredAt(expiredTime)
			.isFile(requestDto.isFile())
			.targetId(requestDto.targetId())
			.permissionType(requestDto.getPermissionType())
			.build();
	}
}

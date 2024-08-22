package com.woowacamp.storage.domain.shredlink.dto.request;

import jakarta.validation.constraints.Positive;

public record CancelSharedLinkRequestDto(
	@Positive long userId,
	boolean isFile,
	@Positive long targetId
) {
}

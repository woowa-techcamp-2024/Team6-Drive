package com.woowacamp.storage.domain.shredlink.dto.request;

import jakarta.validation.constraints.Positive;

public record MakeSharedLinkRequestDto(
	@Positive long userId,
	boolean isFile,
	@Positive long targetId
) {
}

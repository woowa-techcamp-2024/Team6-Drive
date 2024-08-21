package com.woowacamp.storage.domain.shredlink.dto.request;

import com.woowacamp.storage.domain.shredlink.entity.PermissionType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MakeSharedLinkRequestDto(
	@Positive long userId,
	boolean isFile,
	@Positive long targetId,
	@NotNull String permissionType
) {

	public PermissionType getPermissionType() {
		return PermissionType.fromValue(permissionType);
	}
}

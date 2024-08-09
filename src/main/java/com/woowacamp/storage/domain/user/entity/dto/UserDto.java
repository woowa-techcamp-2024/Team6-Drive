package com.woowacamp.storage.domain.user.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserDto {
	private Long id;
	private Long rootFolderId;
	private String userName;

	@Builder
	private UserDto(Long id, Long rootFolderId, String userName) {
		this.id = id;
		this.rootFolderId = rootFolderId;
		this.userName = userName;
	}
}

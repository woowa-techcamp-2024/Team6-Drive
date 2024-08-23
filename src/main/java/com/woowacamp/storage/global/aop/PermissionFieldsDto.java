package com.woowacamp.storage.global.aop;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class PermissionFieldsDto {
	private Long userId;
	private Long fileId;
	private Long folderId;
	private Long moveFolderId; // 파일이나 폴더 이동의 경우 이동할 폴더의 권한도 검증해야 한다.
	private Long ownerId;
	private Long creatorId;

}

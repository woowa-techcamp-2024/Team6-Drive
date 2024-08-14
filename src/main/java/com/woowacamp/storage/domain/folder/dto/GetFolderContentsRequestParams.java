package com.woowacamp.storage.domain.folder.dto;

import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GetFolderContentsRequestParams(
	@NotNull @Positive Long userId,
	@NotNull @Positive Long cursorId,
	@NotNull CursorType cursorType,
	@Min(0) @Max(MAX_SIZE) int size,
	FolderContentsSortField sortBy,
	Sort.Direction sortDirection
) {
	private static final int MAX_SIZE = 1000;
	private static final int DEFAULT_SIZE = 100;

	// 기본 생성자 정의 (모든 필드에 대한 기본값 설정)
	public GetFolderContentsRequestParams {
		if (size == 0) {
			size = DEFAULT_SIZE;
		}
		if (sortBy == null) {
			sortBy = FolderContentsSortField.CREATED_AT;
		}
		if (sortDirection == null) {
			sortDirection = Sort.Direction.DESC;
		}
	}
	// 필요한 경우 추가 메서드를 여기에 정의할 수 있습니다.
}

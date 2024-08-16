package com.woowacamp.storage.domain.folder.dto;

import java.time.LocalDateTime;

import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GetFolderContentsRequestParams(@NotNull @Positive Long userId, @NotNull @Positive Long cursorId,
											 @NotNull CursorType cursorType, @Min(0) @Max(MAX_SIZE) int limit,
											 FolderContentsSortField sortBy, Sort.Direction sortDirection,
											 LocalDateTime localDateTime, Long size) {
	private static final int MAX_SIZE = 1000;
	private static final int DEFAULT_SIZE = 100;

	// 기본 생성자 정의
	public GetFolderContentsRequestParams {
		if (limit == 0) {
			limit = DEFAULT_SIZE;
		}
		if (sortBy == null) {
			sortBy = FolderContentsSortField.CREATED_AT;
		}
		if (sortDirection == null) {
			sortDirection = Sort.Direction.DESC;
		}

		if (isFirstPage(localDateTime, size)) {
			switch (sortBy) {
				case CREATED_AT:
					if (sortDirection.isAscending()) {
						localDateTime = LocalDateTime.of(1970, 1, 1, 0, 0);
					} else {
						localDateTime = LocalDateTime.now().plusYears(1000);
					}
					break;
				case DATA_SIZE:
					if (sortDirection.isAscending()) {
						size = Long.MAX_VALUE;
					} else {
						size = 0L;
					}
					break;
				default:
					throw new IllegalArgumentException("Unsupported sortBy option: " + sortBy);
			}
		}
	}

	private boolean isFirstPage(LocalDateTime localDateTime, Long size) {
		return localDateTime == null || size == null;
	}
}

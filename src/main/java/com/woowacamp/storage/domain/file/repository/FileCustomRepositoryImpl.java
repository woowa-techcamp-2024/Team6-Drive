package com.woowacamp.storage.domain.file.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.file.entity.QFileMetadata;
import com.woowacamp.storage.domain.folder.dto.FolderContentsSortField;
import com.woowacamp.storage.global.constant.UploadStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FileCustomRepositoryImpl implements FileCustomRepository {
	private static final QFileMetadata fileMetadata = QFileMetadata.fileMetadata;
	private final JPAQueryFactory queryFactory;

	public List<FileMetadata> selectFilesWithPagination(long parentId, long cursorId,
		FolderContentsSortField sortBy, Sort.Direction direction, int limit, LocalDateTime time, Long size) {

		// 기본 쿼리 구성 , where 조건 parentFolderId, uploadStatus
		JPAQuery<FileMetadata> query = queryFactory.selectFrom(fileMetadata)
			.where(fileMetadata.parentFolderId.eq(parentId))
			.where(fileMetadata.uploadStatus.eq(UploadStatus.SUCCESS))
			.where(fileMetadata.id.gt(cursorId));

		// 정렬 조건 적용
		OrderSpecifier<?> orderSpecifier;
		switch (sortBy) {
			case CREATED_AT ->
				orderSpecifier = direction.isAscending() ? fileMetadata.createdAt.asc() : fileMetadata.createdAt.desc();
			case FOLDER_SIZE ->
				orderSpecifier = direction.isAscending() ? fileMetadata.size.asc() : fileMetadata.size.desc();
			default ->
				// 기본적으로 ID로 정렬
				orderSpecifier = fileMetadata.id.asc();
		}

		// 정렬 조건 추가 (선택된 필드로 정렬 후, ID로 추가 정렬하여 일관성 유지)
		query = query.orderBy(orderSpecifier, fileMetadata.id.asc());
		// limit 추가
		query.limit(limit);

		return query.fetch();
	}
}

package com.woowacamp.storage.domain.folder.repository;

import java.util.List;

import org.springframework.data.domain.Sort;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.woowacamp.storage.domain.folder.dto.FolderContentsSortField;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;
import com.woowacamp.storage.domain.folder.entity.QFolderMetadata;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FolderCustomRepositoryImpl implements FolderCustomRepository {

	private final JPAQueryFactory queryFactory;

	public List<FolderMetadata> selectFoldersWithPagination(long parentId, long cursorId,
		FolderContentsSortField sortBy, Sort.Direction direction, int size) {
		QFolderMetadata folderMetadata = QFolderMetadata.folderMetadata;

		// 기본 쿼리 구성 , where 조건 parentFolderId, uploadStatus
		JPAQuery<FolderMetadata> query = queryFactory.selectFrom(folderMetadata)
			.where(folderMetadata.parentFolderId.eq(parentId))
			.where(folderMetadata.id.gt(cursorId));

		// 정렬 조건 적용
		OrderSpecifier<?> orderSpecifier;
		switch (sortBy) {
			case CREATED_AT -> orderSpecifier =
				direction.isAscending() ? folderMetadata.createdAt.asc() : folderMetadata.createdAt.desc();
			case FOLDER_SIZE ->
				orderSpecifier = direction.isAscending() ? folderMetadata.size.asc() : folderMetadata.size.desc();
			default ->
				// 기본적으로 ID로 정렬
				orderSpecifier = folderMetadata.id.asc();
		}

		// 정렬 조건 추가 (선택된 필드로 정렬 후, ID로 추가 정렬하여 일관성 유지)
		query = query.orderBy(orderSpecifier, folderMetadata.id.asc());
		// size 추가
		query.limit(size);

		return query.fetch();
	}
}

package com.woowacamp.storage.domain.folder.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
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
		FolderContentsSortField sortBy, Sort.Direction direction, int limit, LocalDateTime dateTime, Long size) {
		QFolderMetadata folderMetadata = QFolderMetadata.folderMetadata;

		// 기본 쿼리 구성
		JPAQuery<FolderMetadata> query = queryFactory.selectFrom(folderMetadata)
			.where(folderMetadata.parentFolderId.eq(parentId));

		// 서브쿼리로 cursorId에 해당하는 생성 시간 또는 폴더 크기 가져오기
		QFolderMetadata cursorFolder = new QFolderMetadata("cursorFolder");

		// 커서 조건 및 정렬 조건 설정
		BooleanExpression cursorCondition;
		OrderSpecifier<?> orderSpecifier;
		switch (sortBy) {
			case CREATED_AT:

				if (direction.isAscending()) {
					cursorCondition = folderMetadata.createdAt.gt(dateTime)
						.or(folderMetadata.createdAt.eq(dateTime).and(folderMetadata.id.gt(cursorId)));
					orderSpecifier = folderMetadata.createdAt.asc();
				} else {
					cursorCondition = folderMetadata.createdAt.lt(dateTime)
						.or(folderMetadata.createdAt.eq(dateTime).and(folderMetadata.id.gt(cursorId)));
					orderSpecifier = folderMetadata.createdAt.desc();
				}
				break;
			case FOLDER_SIZE:
				if (direction.isAscending()) {
					cursorCondition = folderMetadata.size.gt(size)
						.or(folderMetadata.size.eq(size).and(folderMetadata.id.gt(cursorId)));
					orderSpecifier = folderMetadata.size.asc();
				} else {
					cursorCondition = folderMetadata.size.lt(size)
						.or(folderMetadata.size.eq(size).and(folderMetadata.id.gt(cursorId)));
					orderSpecifier = folderMetadata.size.desc();
				}
				break;
			default:
				// 조건에 없는 Enum 값이 들어오면 id 기준 정렬
				cursorCondition = folderMetadata.id.gt(cursorId);
				orderSpecifier = folderMetadata.id.asc();
		}

		// 커서 조건 추가
		query = query.where(cursorCondition);

		// 정렬 조건이 같으면 id 기준으로 정렬
		query = query.orderBy(orderSpecifier, folderMetadata.id.asc());

		// limit 제한 추가
		query.limit(limit);

		return query.fetch();
	}

}

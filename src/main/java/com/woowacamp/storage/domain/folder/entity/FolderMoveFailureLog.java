package com.woowacamp.storage.domain.folder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "folder_move_failure_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FolderMoveFailureLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "folder_move_failure_log_id", columnDefinition = "BIGINT")
	private Long id;
	private Long sourceFolderId;
	private Long targetFolderId;

	public FolderMoveFailureLog(Long sourceFolderId, Long targetFolderId) {
		this.sourceFolderId = sourceFolderId;
		this.targetFolderId = targetFolderId;
	}
}

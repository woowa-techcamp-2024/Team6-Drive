package com.woowacamp.storage.domain.file.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_move_failure_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FileMoveFailureLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "file_move_failure_log_id", columnDefinition = "BIGINT")
	private Long id;
	private Long fileId;
	private Long sourceFolderId;
	private Long targetFolderId;
	private LocalDateTime failedAt;
	private boolean processed;

	@Builder
	public FileMoveFailureLog(Long fileId, Long sourceFolderId, Long targetFolderId, LocalDateTime failedAt) {
		this.fileId = fileId;
		this.sourceFolderId = sourceFolderId;
		this.targetFolderId = targetFolderId;
		this.failedAt = failedAt;
		this.processed = false;
	}
}

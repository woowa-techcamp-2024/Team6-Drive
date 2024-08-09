package com.woowacamp.storage.domain.folder.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "folder_metadata"
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FolderMetadata {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "folder_metadata_id", columnDefinition = "BIGINT")
	private Long id;

	@Column(name = "root_id", columnDefinition = "BIGINT")
	private Long rootId;

	@Column(name = "owner_id", columnDefinition = "BIGINT")
	private Long ownerId;

	@Column(name = "creator_id", columnDefinition = "BIGINT")
	private Long creatorId;

	@Column(name = "created_at", columnDefinition = "TIMESTAMP NOT NULL")
	@NotNull
	private LocalDateTime createdAt;

	@Column(name = "updated_at", columnDefinition = "TIMESTAMP NOT NULL")
	@NotNull
	private LocalDateTime updatedAt;

	@Column(name = "parent_folder_id", columnDefinition = "BIGINT")
	private Long parentFolderId;

	@Column(name = "upload_folder_name", columnDefinition = "VARCHAR(100) NOT NULL")
	@NotNull
	private String uploadFolderName;

	@Builder
	private FolderMetadata(Long id, Long rootId, Long ownerId, Long creatorId, LocalDateTime createdAt,
		LocalDateTime updatedAt, Long parentFolderId, String uploadFolderName) {
		this.id = id;
		this.rootId = rootId;
		this.ownerId = ownerId;
		this.creatorId = creatorId;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.parentFolderId = parentFolderId;
		this.uploadFolderName = uploadFolderName;
	}

	public void initOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public void initCreatorId(Long creatorId) {
		this.creatorId = creatorId;
	}
}

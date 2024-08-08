package com.woowacamp.storage.domain.folder.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
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
	@GeneratedValue
	@Column(name = "file_metadata_id", columnDefinition = "BIGINT")
	private Long id;

	@Column(name = "root_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long rootId;

	@Column(name = "creator_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long creatorId;

	@Column(name = "created_at", columnDefinition = "TIMESTAMP NOT NULL")
	@NotNull
	private LocalDateTime createdAt;

	@Column(name = "updated_at", columnDefinition = "TIMESTAMP NOT NULL")
	@NotNull
	private LocalDateTime updatedAt;

	@Column(name = "parent_folder_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long parentFolderId;

	@Column(name = "upload_folder_name", columnDefinition = "VARCHAR(100) NOT NULL")
	@NotNull
	private String uploadFolderName;
}

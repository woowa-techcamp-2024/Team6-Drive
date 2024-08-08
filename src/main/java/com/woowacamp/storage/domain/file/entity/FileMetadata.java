package com.woowacamp.storage.domain.file.entity;

import java.time.LocalDateTime;

import com.woowacamp.storage.global.constant.UploadStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "file_metadata"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FileMetadata {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "file_metadata_id")
	private Long id;

	@Column(name = "root_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long rootId;

	@Column(name = "creator_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long creatorId;

	@Column(name = "file_type", columnDefinition = "VARCHAR(50) NOT NULL")
	@NotNull
	private String fileType;

	@Column(name = "created_at", columnDefinition = "TIMESTAMP NOT NULL")
	@NotNull
	private LocalDateTime createdAt;

	@Column(name = "updated_at", columnDefinition = "TIMESTAMP NOT NULL")
	@NotNull
	private LocalDateTime updatedAt;

	@Column(name = "parent_folder_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long parentFolderId;

	@Column(name = "file_size", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long fileSize;

	@Column(name = "upload_file_name", columnDefinition = "VARCHAR(100) NOT NULL")
	@NotNull
	private String uploadFileName;

	@Column(name = "uuid_file_name", columnDefinition = "VARCHAR(100) NOT NULL")
	@NotNull
	private String uuidFileName;

	@Enumerated(EnumType.STRING)
	@Column(name = "upload_status", columnDefinition = "VARCHAR(30) NOT NULL")
	@NotNull
	private UploadStatus uploadStatus;
}

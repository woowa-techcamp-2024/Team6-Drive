package com.woowacamp.storage.domain.shredlink.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "shared_link"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SharedLink {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "shared_link_id", columnDefinition = "BIGINT")
	private Long id;

	@Column(name = "created_at", columnDefinition = "TIMESTAMP NOT NULL")
	@NotNull
	private LocalDateTime createdAt;

	@Column(name = "metadata_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long metadataId;

	@Column(name = "is_file_link", columnDefinition = "TINYINT NOT NULL")
	@NotNull
	private Boolean isFileLink;

	@Column(name = "expired_at", columnDefinition = "TIMESTAMP NOT NULL")
	@NotNull
	private LocalDateTime expiredAt;
}

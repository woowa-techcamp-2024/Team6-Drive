package com.woowacamp.storage.domain.shredlink.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "shared_link",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"shared_link_url"}),
		@UniqueConstraint(columnNames = {"shared_token"})
	}
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

	// 공유하는 링크
	@Column(name = "shared_link_url", columnDefinition = "VARCHAR(300) NOT NULL")
	@NotNull
	private String sharedLinkUrl;

	// 공유하는 사용자의 pk
	@Column(name = "shared_user_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long sharedUserId;

	// 공유 받은 사용자를 인증할 토큰(쿠키)
	@Column(name = "shared_token", columnDefinition = "VARCHAR(100) NOT NULL")
	@NotNull
	private String sharedToken;

	@Column(name = "expired_at", columnDefinition = "TIMESTAMP NOT NULL")
	@NotNull
	private LocalDateTime expiredAt;

	@Column(name = "is_file", columnDefinition = "TINYINT(1) NOT NULL")
	@NotNull
	private Boolean isFile;

	@Column(name = "target_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long targetId;

	@Column(name = "permission_type", columnDefinition = "VARCHAR(10) NOT NULL")
	@NotNull
	@Enumerated(EnumType.STRING)
	private PermissionType permissionType;

	@Builder

	public SharedLink(Long id, LocalDateTime createdAt, String sharedLinkUrl, Long sharedUserId, String sharedToken,
		LocalDateTime expiredAt, Boolean isFile, Long targetId, PermissionType permissionType) {
		this.id = id;
		this.createdAt = createdAt;
		this.sharedLinkUrl = sharedLinkUrl;
		this.sharedUserId = sharedUserId;
		this.sharedToken = sharedToken;
		this.expiredAt = expiredAt;
		this.isFile = isFile;
		this.targetId = targetId;
		this.permissionType = permissionType;
	}
}

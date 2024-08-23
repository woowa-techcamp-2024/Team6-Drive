package com.woowacamp.storage.domain.shredlink.entity;

import java.time.LocalDateTime;

import com.woowacamp.storage.global.constant.PermissionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shared_link",
	uniqueConstraints = {@UniqueConstraint(columnNames = {"shared_id"})},
	indexes = {@Index(name = "target_index", columnList = "is_file,target_id")})
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

	// 공유 링크로 접속시 리다이렉트할 url
	@Column(name = "redirect_url", columnDefinition = "VARCHAR(300) NOT NULL")
	@NotNull
	private String redirectUrl;

	@Column(name = "shared_id", columnDefinition = "VARCHAR(100) NOT NULL")
	private String sharedId;

	// 공유하는 사용자의 pk
	@Column(name = "shared_user_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long sharedUserId;

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
	public SharedLink(Long id, LocalDateTime createdAt, String redirectUrl, String sharedId, Long sharedUserId,
		LocalDateTime expiredAt, Boolean isFile, Long targetId, PermissionType permissionType) {
		this.id = id;
		this.createdAt = createdAt;
		this.redirectUrl = redirectUrl;
		this.sharedUserId = sharedUserId;
		this.sharedId = sharedId;
		this.expiredAt = expiredAt;
		this.isFile = isFile;
		this.targetId = targetId;
		this.permissionType = permissionType;
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiredAt);
	}
}

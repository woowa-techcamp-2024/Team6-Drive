package com.woowacamp.storage.domain.user.entity;

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
	name = "users"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id", columnDefinition = "BIGINT")
	private Long id;

	@Column(name = "user_name", columnDefinition = "VARCHAR(20)")
	@NotNull
	private String userName;

	@Column(name = "root_folder_id", columnDefinition = "BIGINT NOT NULL")
	@NotNull
	private Long rootFolderId;

	@Builder
	private User(Long id, String userName, Long rootFolderId) {
		this.id = id;
		this.userName = userName;
		this.rootFolderId = rootFolderId;
	}
}

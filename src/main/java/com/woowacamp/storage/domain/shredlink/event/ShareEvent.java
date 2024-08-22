package com.woowacamp.storage.domain.shredlink.event;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEvent;

import com.woowacamp.storage.domain.shredlink.entity.SharedLink;
import com.woowacamp.storage.global.constant.PermissionType;

public class ShareEvent extends ApplicationEvent {
	private final SharedLink sharedLink;

	public ShareEvent(Object source, SharedLink sharedLink) {
		super(source);
		this.sharedLink = sharedLink;
	}

	public boolean isTargetFile() {
		return sharedLink.getIsFile();
	}

	public Long targetId() {
		return sharedLink.getTargetId();
	}

	public PermissionType permissionType() {
		return sharedLink.getPermissionType();
	}

	public LocalDateTime sharingExpireAt() {
		return sharedLink.getExpiredAt();
	}
}

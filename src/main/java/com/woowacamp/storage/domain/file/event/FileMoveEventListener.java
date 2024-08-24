package com.woowacamp.storage.domain.file.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.woowacamp.storage.domain.shredlink.service.SharedLinkService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileMoveEventListener {
	private final SharedLinkService sharingService;

	@EventListener
	public void updateSubSharingStatus(FileMoveEvent moveEvent) {
		sharingService.updateFileShareStatus(moveEvent.getSourceFile().getId(),
			moveEvent.getTargetFolder().getPermissionType(), moveEvent.getTargetFolder().getSharingExpiredAt());
	}
}

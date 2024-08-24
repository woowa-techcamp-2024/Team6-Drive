package com.woowacamp.storage.domain.folder.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.woowacamp.storage.domain.shredlink.service.SharedLinkService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FolderMoveEventListener {
	private final SharedLinkService sharingService;

	@EventListener
	public void updateSubSharingStatus(FolderMoveEvent moveEvent) {
		sharingService.updateFolderSharingStatus(moveEvent.getSourceFolder().getId(),
			moveEvent.getTargetFolder().getPermissionType(), moveEvent.getTargetFolder().getSharingExpiredAt());
	}
}

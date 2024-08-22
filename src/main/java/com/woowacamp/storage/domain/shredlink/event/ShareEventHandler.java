package com.woowacamp.storage.domain.shredlink.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.woowacamp.storage.domain.file.service.FileService;
import com.woowacamp.storage.domain.folder.service.FolderService;

@Component
public class ShareEventHandler {
	private final FolderService folderService;
	private final FileService fileService;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public ShareEventHandler(FolderService folderService, FileService fileService) {
		this.folderService = folderService;
		this.fileService = fileService;
	}

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener
	public void updateShareStatus(ShareEvent shareEvent) {
		if (shareEvent.isTargetFile()) {
			fileService.updateShareStatus(shareEvent.targetId(), shareEvent.permissionType(),
				shareEvent.sharingExpireAt());
		} else {
			long updated = folderService.updateSubFolderShareStatus(shareEvent.targetId(), shareEvent.permissionType(),
				shareEvent.sharingExpireAt());
			log.info("total updated folders and files count: {}", updated);
		}
	}
}

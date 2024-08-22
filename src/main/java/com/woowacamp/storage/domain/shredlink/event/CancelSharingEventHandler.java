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

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CancelSharingEventHandler {
	private final FolderService folderService;
	private final FileService fileService;
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener
	public void updateShareStatus(CancelSharingEvent cancelSharingEvent) {
		if (cancelSharingEvent.isTargetFile()) {
			fileService.cancelShare(cancelSharingEvent.getTargetId());
		} else {
			long updated = folderService.cancelShare(cancelSharingEvent.getTargetId());
			log.info("total folders and files count which has cancelled sharing: {}", updated);
		}
	}
}

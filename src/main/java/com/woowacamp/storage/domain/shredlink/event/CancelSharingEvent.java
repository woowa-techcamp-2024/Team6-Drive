package com.woowacamp.storage.domain.shredlink.event;

import org.springframework.context.ApplicationEvent;

public class CancelSharingEvent extends ApplicationEvent {
	private final boolean isFile;
	private final Long tagetId;

	public CancelSharingEvent(Object source, boolean isFile, Long tagetId) {
		super(source);
		this.isFile = isFile;
		this.tagetId = tagetId;
	}

	public boolean isTargetFile() {
		return isFile;
	}

	public Long getTargetId() {
		return tagetId;
	}
}

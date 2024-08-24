package com.woowacamp.storage.domain.folder.event;

import org.springframework.context.ApplicationEvent;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;

import lombok.Getter;

@Getter
public class FolderMoveEvent extends ApplicationEvent {
	private final FolderMetadata sourceFolder;
	private final FolderMetadata targetFolder;

	public FolderMoveEvent(Object source, FolderMetadata sourceFolder, FolderMetadata targetFolder) {
		super(source);
		this.sourceFolder = sourceFolder;
		this.targetFolder = targetFolder;
	}
}

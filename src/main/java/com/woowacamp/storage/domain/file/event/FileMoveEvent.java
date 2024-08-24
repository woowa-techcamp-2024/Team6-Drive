package com.woowacamp.storage.domain.file.event;

import org.springframework.context.ApplicationEvent;

import com.woowacamp.storage.domain.file.entity.FileMetadata;
import com.woowacamp.storage.domain.folder.entity.FolderMetadata;

import lombok.Getter;

@Getter
public class FileMoveEvent extends ApplicationEvent {
	private final FileMetadata sourceFile;
	private final FolderMetadata targetFolder;

	public FileMoveEvent(Object source, FileMetadata sourceFile, FolderMetadata targetFolder) {
		super(source);
		this.sourceFile = sourceFile;
		this.targetFolder = targetFolder;
	}
}

package com.woowacamp.storage.domain.folder.repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.woowacamp.storage.domain.folder.entity.FolderMetadata;

public interface FolderMetadataJpaRepository extends JpaRepository<FolderMetadata, Long> {
}

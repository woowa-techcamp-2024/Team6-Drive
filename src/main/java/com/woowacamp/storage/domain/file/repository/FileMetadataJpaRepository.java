package com.woowacamp.storage.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.woowacamp.storage.domain.file.entity.FileMetadata;

public interface FileMetadataJpaRepository extends JpaRepository<FileMetadata, Long> {
}

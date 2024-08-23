package com.woowacamp.storage.domain.shredlink.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.woowacamp.storage.domain.shredlink.entity.SharedLink;

public interface SharedLinkRepository extends JpaRepository<SharedLink, Long> {
	Optional<SharedLink> findBySharedId(String shareId);

	List<SharedLink> findByIsFileAndTargetId(boolean isFile, long targetId);

	void deleteByIsFileAndTargetId(boolean isFile, Long targetId);
}

package com.woowacamp.storage.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.woowacamp.storage.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}

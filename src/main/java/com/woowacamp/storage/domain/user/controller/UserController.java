package com.woowacamp.storage.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.user.entity.dto.UserDto;
import com.woowacamp.storage.domain.user.entity.dto.request.CreateUserReqDto;
import com.woowacamp.storage.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	@GetMapping
	@RequestMapping("/{userId}")
	public ResponseEntity<UserDto> getUser(@PathVariable Long userId) {
		return ResponseEntity.ok(userService.findById(userId));
	}

	@PostMapping
	@RequestMapping
	public ResponseEntity<UserDto> createUser(@RequestBody CreateUserReqDto req) {
		return ResponseEntity.ok(userService.save(req));
	}
}

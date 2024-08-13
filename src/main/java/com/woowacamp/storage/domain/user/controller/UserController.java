package com.woowacamp.storage.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.user.dto.UserDto;
import com.woowacamp.storage.domain.user.dto.request.CreateUserReqDto;
import com.woowacamp.storage.domain.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	@GetMapping("/{userId}")
	public UserDto getUser(@PathVariable long userId) {
		return userService.findById(userId);
	}

	@PostMapping
	@ResponseStatus(value = HttpStatus.CREATED)
	public UserDto createUser(@RequestBody @Valid CreateUserReqDto req) {
		return userService.save(req);
	}
}

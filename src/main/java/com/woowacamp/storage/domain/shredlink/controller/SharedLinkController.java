package com.woowacamp.storage.domain.shredlink.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.shredlink.dto.request.MakeSharedLinkRequestDto;
import com.woowacamp.storage.domain.shredlink.dto.response.SharedLinkResponseDto;
import com.woowacamp.storage.domain.shredlink.service.SharedLinkService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/share")
public class SharedLinkController {

	private final SharedLinkService sharedLinkService;

	@PostMapping
	public SharedLinkResponseDto createSharedLink(@Valid @RequestBody MakeSharedLinkRequestDto requestDto) {
		return sharedLinkService.createSharedLink(requestDto);
	}
}

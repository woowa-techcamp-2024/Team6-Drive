package com.woowacamp.storage.domain.shredlink.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.woowacamp.storage.domain.shredlink.dto.request.MakeSharedLinkRequestDto;
import com.woowacamp.storage.domain.shredlink.dto.response.SharedLinkResponseDto;
import com.woowacamp.storage.domain.shredlink.service.SharedLinkService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/share")
public class SharedLinkController {

	private final SharedLinkService sharedLinkService;

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public SharedLinkResponseDto createSharedLink(@Valid @RequestBody MakeSharedLinkRequestDto requestDto) {
		return sharedLinkService.createShareLink(requestDto);
	}
}

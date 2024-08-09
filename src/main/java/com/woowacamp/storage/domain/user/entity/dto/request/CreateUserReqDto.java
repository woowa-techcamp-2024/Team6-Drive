package com.woowacamp.storage.domain.user.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CreateUserReqDto {

	@NotBlank
	@Size(min = 3, max = 20)
	private String userName;
}

package com.woowacamp.storage.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

public record CreateUserReqDto(@NotBlank @Size(min = 3, max = 20) String userName) {
}

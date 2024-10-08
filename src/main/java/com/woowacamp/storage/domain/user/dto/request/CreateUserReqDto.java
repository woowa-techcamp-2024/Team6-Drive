package com.woowacamp.storage.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record CreateUserReqDto(@Size(min = 3, max = 20) String userName) {
}

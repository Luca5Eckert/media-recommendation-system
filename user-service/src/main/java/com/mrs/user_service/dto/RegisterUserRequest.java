package com.mrs.user_service.dto;

import com.mrs.user_service.model.RoleUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password
) {
}

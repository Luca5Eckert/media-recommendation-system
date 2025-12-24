package com.mrs.user_service.dto;

import com.mrs.user_service.model.Genre;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateUserPrefenceRequest(
        @NotBlank List<Genre> genres
) {
}

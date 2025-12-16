package com.mrs.user_service.dto;

import java.util.UUID;

public record UserGetResponse(
        UUID id,
        String name,
        String email
) {
}

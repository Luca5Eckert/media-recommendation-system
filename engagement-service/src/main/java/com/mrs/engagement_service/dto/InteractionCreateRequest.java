package com.mrs.engagement_service.dto;

import com.mrs.engagement_service.model.InteractionType;

import java.util.UUID;

public record InteractionCreateRequest(
        UUID userId,
        UUID movieId,
        InteractionType type
) {
}

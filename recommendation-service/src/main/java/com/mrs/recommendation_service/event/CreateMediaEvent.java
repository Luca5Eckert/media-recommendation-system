package com.mrs.recommendation_service.event;

import java.util.List;
import java.util.UUID;

public record CreateMediaEvent(
        UUID mediaId,
        List<String> genres
) {
}

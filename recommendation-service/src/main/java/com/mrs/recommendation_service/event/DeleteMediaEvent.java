package com.mrs.recommendation_service.event;

import java.util.UUID;

public record DeleteMediaEvent(
        UUID mediaId
) {
}

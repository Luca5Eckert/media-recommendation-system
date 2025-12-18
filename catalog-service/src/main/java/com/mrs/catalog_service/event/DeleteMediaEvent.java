package com.mrs.catalog_service.event;

import java.util.UUID;

public record DeleteMediaEvent(
        UUID mediaId
) {
}

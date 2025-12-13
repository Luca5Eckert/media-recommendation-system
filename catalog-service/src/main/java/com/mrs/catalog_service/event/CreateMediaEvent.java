package com.mrs.catalog_service.event;

import com.mrs.catalog_service.model.Genre;

import java.util.List;
import java.util.UUID;

public record CreateMediaEvent(
        UUID mediaId,
        List<Genre> genres
) {
}

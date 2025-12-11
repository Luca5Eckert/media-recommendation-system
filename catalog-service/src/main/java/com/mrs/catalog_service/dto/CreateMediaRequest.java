package com.mrs.catalog_service.dto;

import com.mrs.catalog_service.model.Genre;

import java.util.List;

public record CreateMediaRequest(
        String name,
        double timeInMinutes,
        List<Genre> genres
) {
}

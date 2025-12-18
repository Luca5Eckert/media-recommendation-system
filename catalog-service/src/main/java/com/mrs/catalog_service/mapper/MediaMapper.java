package com.mrs.catalog_service.mapper;

import com.mrs.catalog_service.dto.GetMediaResponse;
import com.mrs.catalog_service.model.Media;


public class MediaMapper {

    public GetMediaResponse toGetResponse(Media media){
        return new GetMediaResponse(
                media.getId(),
                media.getTitle(),
                media.getDescription(),
                media.getReleaseYear(),
                media.getMediaType(),
                media.getCoverUrl(),
                media.getGenres(),
                media.getCreateAt(),
                media.getUpdateAt()
        );

    }
}

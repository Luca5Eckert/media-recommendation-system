package com.mrs.catalog_service.service;

import com.mrs.catalog_service.dto.CreateMediaRequest;
import com.mrs.catalog_service.handler.CreateMediaHandler;
import com.mrs.catalog_service.model.Media;
import org.springframework.stereotype.Service;

@Service
public class MediaService {

    private final CreateMediaHandler createMediaHandler;

    public MediaService(CreateMediaHandler createMediaHandler) {
        this.createMediaHandler = createMediaHandler;
    }

    public void create(CreateMediaRequest createMediaRequest){
        Media media = new Media(
                createMediaRequest.name(),
                createMediaRequest.timeInMinutes(),
                createMediaRequest.genres()
        );

        createMediaHandler.handler(media);
    }



}

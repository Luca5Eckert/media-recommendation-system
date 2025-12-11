package com.mrs.catalog_service.handler;

import com.mrs.catalog_service.model.Media;
import com.mrs.catalog_service.repository.MediaRepository;
import org.springframework.stereotype.Component;

@Component
public class CreateMediaHandler {

    private final MediaRepository mediaRepository;

    public CreateMediaHandler(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    public void handler(Media media){
        if(media == null) throw new IllegalArgumentException("Media can't be null");

        mediaRepository.save(media);
    }

}

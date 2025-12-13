package com.mrs.catalog_service.handler;

import com.mrs.catalog_service.model.Media;
import com.mrs.catalog_service.repository.MediaRepository;
import com.mrs.catalog_service.event.CreateMediaEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CreateMediaHandler {

    private final MediaRepository mediaRepository;

    private final KafkaTemplete<String, CreateMediaEvent> kafka;


    public CreateMediaHandler(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    public void handler(Media media){
        if(media == null) throw new IllegalArgumentException("Media can't be null");

        mediaRepository.save(media);

        CreateMediaEvent createMediaEvent = new CreateMediaHandler(
                media.getId(),
                media.getGenres()
        );

        kafka.send("create-media", createMediaEvent.mediaId(), createMediaEvent);
    }

}

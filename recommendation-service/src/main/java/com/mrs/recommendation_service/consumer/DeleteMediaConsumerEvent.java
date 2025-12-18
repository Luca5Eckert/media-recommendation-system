package com.mrs.recommendation_service.consumer;

import com.mrs.recommendation_service.event.DeleteMediaEvent;
import com.mrs.recommendation_service.repository.MediaFeatureRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeleteMediaConsumerEvent {

    private final MediaFeatureRepository mediaFeatureRepository;

    public DeleteMediaConsumerEvent(MediaFeatureRepository mediaFeatureRepository) {
        this.mediaFeatureRepository = mediaFeatureRepository;
    }

    @KafkaListener(topics = "delete_media")
    public void listen(DeleteMediaEvent deleteMediaEvent){
        mediaFeatureRepository.deleteById(deleteMediaEvent.mediaId());
    }
}

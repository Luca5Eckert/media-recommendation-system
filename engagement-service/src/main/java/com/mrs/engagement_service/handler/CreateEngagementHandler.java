package com.mrs.engagement_service.handler;

import com.mrs.engagement_service.model.Interaction;
import com.mrs.engagement_service.repository.EngagementRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateEngagementHandler {

    private final EngagementRepository engagementRepository;

    private final KafkaTemplate<UUID, Interaction> kafka;

    public CreateEngagementHandler(EngagementRepository engagementRepository, KafkaTemplate<UUID, Interaction> kafka) {
        this.engagementRepository = engagementRepository;
        this.kafka = kafka;
    }

    public void handler(Interaction interaction){
        if(interaction == null) throw new IllegalArgumentException("Interaction can't be null");

        kafka.send("create-engagement", interaction.getUserId(), interaction);

        engagementRepository.save(interaction);
    }

}

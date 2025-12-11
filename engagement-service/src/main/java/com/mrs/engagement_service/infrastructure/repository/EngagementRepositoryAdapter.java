package com.mrs.engagement_service.infrastructure.repository;

import com.mrs.engagement_service.model.Interaction;
import com.mrs.engagement_service.repository.EngagementRepository;
import org.springframework.stereotype.Component;

@Component
public class EngagementRepositoryAdapter implements EngagementRepository {

    public final EngagementRepositoryJpa engagementRepositoryJpa;

    public EngagementRepositoryAdapter(EngagementRepositoryJpa engagementRepositoryJpa) {
        this.engagementRepositoryJpa = engagementRepositoryJpa;
    }

    @Override
    public void save(Interaction interaction) {
        engagementRepositoryJpa.save(interaction);
    }

}

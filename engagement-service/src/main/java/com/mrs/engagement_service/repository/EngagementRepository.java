package com.mrs.engagement_service.repository;

import com.mrs.engagement_service.model.Interaction;

public interface EngagementRepository {
    void save(Interaction interaction);
}

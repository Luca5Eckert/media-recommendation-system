package com.mrs.recommendation_service.model;

import com.mrs.recommendation_service.event.InteractionEvent;
import jakarta.persistence.*; // Ou javax.persistence dependendo da versão do Spring Boot
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    private UUID userId;

    @Version
    private Long version;

    @ElementCollection
    private Map<String, Double> genreScores = new HashMap<>();

    @ElementCollection
    private Set<UUID> interactedMediaIds = new HashSet<>();

    private long totalLikes;
    private long totalDislikes;
    private long totalWatches;
    private double totalEngagementScore;
    private Instant lastUpdated;

    public UserProfile(UUID userId) {
        this.userId = userId;
        this.lastUpdated = Instant.now();
    }

    public void processInteraction(MediaFeature media, InteractionType type, double interactionValue) {
        if (media.getGenres() != null) {
            for (String genre : media.getGenres()) {
                this.genreScores.merge(genre, type.getWeightInteraction(), Double::sum);
            }
        }

        this.interactedMediaIds.add(media.getMediaId());

        updateCounters(type);

        updateEngagementScore(type, interactionValue);

        this.lastUpdated = Instant.now();
    }

    private void updateCounters(InteractionType type) {
        switch (type) {
            case LIKE -> this.totalLikes++;
            case DISLIKE -> this.totalDislikes++;
            case WATCH -> this.totalWatches++;
        }
    }

    private void updateEngagementScore(InteractionType type, double interactionValue) {
        double weight = type.getWeightInteraction();

        double scoreIncrement = weight * (1 + interactionValue);
        this.totalEngagementScore += scoreIncrement;
    }
}
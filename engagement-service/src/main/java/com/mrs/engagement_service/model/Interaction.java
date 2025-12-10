package com.mrs.engagement_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    @Column(nullable = false)
    private final UUID userId;

    @Column(nullable = false)
    private Long movieId;

    @Enumerated(EnumType.STRING)
    private InteractionType type;

    private LocalDateTime timestamp;

    public Interaction() {
        this.id = null;
        this.userId = null;
    }

}

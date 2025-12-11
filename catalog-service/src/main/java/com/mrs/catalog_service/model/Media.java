package com.mrs.catalog_service.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_media")
@Data
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    private String name;

    private double timeInMinutes;

    @ElementCollection
    @CollectionTable(
            name = "tb_media_genre",
            joinColumns = @JoinColumn(name = "media_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "genre_name")
    private List<Genre> genres;

    public Media() {
    }

    public Media(String name, double timeInMinutes, List<Genre> genres) {
        this.name = name;
        this.timeInMinutes = timeInMinutes;
        this.genres = genres;
    }

}

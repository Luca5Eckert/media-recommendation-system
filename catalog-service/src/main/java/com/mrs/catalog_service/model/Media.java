package com.mrs.catalog_service.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "medias")
@Data
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    private String title;

    private String description;

    private int releaseYear;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    private String cover_url;

    @Version
    private long version;

    private Instant createAt;

    private Instant updateAt;

    private Instant deletedAt;

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


}

package com.mrs.recommendation_service.model;


import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "medias_features")
@Data
public class MediaFeature {

    @Id
    private UUID mediaId;

    @Type(ListArrayType.class)
    @Column(name = "genres", columnDefinition = "text[]")
    private List<String> genres;

    private double popularityScore;

    public MediaFeature(){
    }

}
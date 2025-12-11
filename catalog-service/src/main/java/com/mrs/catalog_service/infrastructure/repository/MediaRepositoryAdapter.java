package com.mrs.catalog_service.infrastructure.repository;

import com.mrs.catalog_service.model.Media;
import com.mrs.catalog_service.repository.MediaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MediaRepositoryAdapter implements MediaRepository {

    private final MediaRepositoryJpa mediaRepositoryJpa;

    public MediaRepositoryAdapter(MediaRepositoryJpa mediaRepositoryJpa) {
        this.mediaRepositoryJpa = mediaRepositoryJpa;
    }

    @Override
    public void save(Media media) {
        mediaRepositoryJpa.save(media);
    }

}

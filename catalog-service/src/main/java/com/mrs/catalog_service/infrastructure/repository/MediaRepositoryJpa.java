package com.mrs.catalog_service.infrastructure.repository;

import com.mrs.catalog_service.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaRepositoryJpa extends JpaRepository<Media, UUID> {
}

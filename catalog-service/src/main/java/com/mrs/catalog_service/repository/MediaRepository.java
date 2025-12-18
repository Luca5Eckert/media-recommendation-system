package com.mrs.catalog_service.repository;

import com.mrs.catalog_service.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {
}

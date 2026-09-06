package com.vellumhub.recommendation_service.module.book_feature.infrastructure.repository;

import com.vellumhub.recommendation_service.module.book_feature.domain.model.BookFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBookFeatureRepository extends JpaRepository<BookFeature, UUID> {

    @Query(value = RankingQueries.SEMANTIC_POPULARITY, nativeQuery = true)
    List<UUID> findTopVectorRecommendations(
            @Param("userId") UUID userId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = RankingQueries.GLOBAL_POPULARITY, nativeQuery = true)
    List<UUID> findMostPopularMedias(
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}

package com.vellumhub.recommendation_service.module.book_feature.infrastructure.repository;

/**
 * Canonical SQL used by recommendation ranking and by the offline evaluation harness.
 * Keeping the current ranker query here prevents the benchmark from reimplementing
 * the production scoring formula independently.
 */
public final class RankingQueries {

    public static final int CANDIDATE_POOL_SIZE = 200;
    public static final double SEMANTIC_WEIGHT = 0.7;
    public static final double POPULARITY_WEIGHT = 0.3;

    private RankingQueries() {
    }

    private static final String CANDIDATES = """
            WITH user_data AS (
                SELECT
                    profile_vector,
                    COALESCE(interacted_book_ids, '{}'::uuid[]) AS interacted_ids
                FROM user_profiles
                WHERE user_id = :userId
            ),
            candidates AS (
                SELECT
                    b.book_id,
                    b.popularity_score,
                    (b.embedding <=> u.profile_vector) AS vector_dist
                FROM book_features b
                CROSS JOIN user_data u
                WHERE b.book_id <> ALL(u.interacted_ids)
                ORDER BY b.embedding <=> u.profile_vector ASC, b.book_id ASC
                LIMIT 200
            )
            """;

    public static final String SEMANTIC_ONLY = CANDIDATES + """
            SELECT c.book_id
            FROM candidates c
            ORDER BY c.vector_dist ASC, c.book_id ASC
            LIMIT :limit OFFSET :offset
            """;

    public static final String SEMANTIC_POPULARITY = CANDIDATES + """
            SELECT c.book_id
            FROM candidates c
            ORDER BY
                (c.vector_dist * 0.7) + ((1 - COALESCE(c.popularity_score, 0)) * 0.3) ASC,
                c.book_id ASC
            LIMIT :limit OFFSET :offset
            """;

    public static final String POPULARITY_ONLY_FOR_USER = CANDIDATES + """
            SELECT c.book_id
            FROM candidates c
            ORDER BY c.popularity_score DESC, c.book_id ASC
            LIMIT :limit OFFSET :offset
            """;

    public static final String GLOBAL_POPULARITY = """
            SELECT b.book_id
            FROM book_features b
            ORDER BY b.popularity_score DESC, b.book_id ASC
            LIMIT :limit OFFSET :offset
            """;
}

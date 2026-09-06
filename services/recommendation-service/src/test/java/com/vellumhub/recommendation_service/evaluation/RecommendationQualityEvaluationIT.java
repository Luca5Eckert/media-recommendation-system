package com.vellumhub.recommendation_service.evaluation;

import com.vellumhub.recommendation_service.evaluation.SyntheticEvaluationDataset.Dataset;
import com.vellumhub.recommendation_service.evaluation.SyntheticEvaluationDataset.EvaluationBook;
import com.vellumhub.recommendation_service.evaluation.SyntheticEvaluationDataset.EvaluationUser;
import com.vellumhub.recommendation_service.module.book_feature.infrastructure.repository.RankingQueries;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("evaluation")
@Testcontainers
class RecommendationQualityEvaluationIT {

    private static final int K = 10;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg15").asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("recommendation_evaluation")
            .withUsername("evaluation")
            .withPassword("evaluation");

    @Test
    void evaluatesCurrentRankerAgainstExplicitBaselines() throws Exception {
        Dataset dataset = SyntheticEvaluationDataset.generate();
        assertThat(dataset.books().size() - SyntheticEvaluationDataset.PROFILE_INTERACTIONS_PER_USER)
                .as("v1 evaluation catalog must fit inside the production candidate pool")
                .isLessThanOrEqualTo(RankingQueries.CANDIDATE_POOL_SIZE);

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        );
        Flyway.configure().dataSource(dataSource).load().migrate();
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(dataSource);

        Map<UUID, EvaluationBook> booksById = SyntheticEvaluationDataset.booksById(dataset);
        persistDataset(jdbc, dataset, booksById);

        List<EvaluationReport.UserResult> perUser = new ArrayList<>();
        Map<EvaluationRanker, Set<UUID>> coverage = new EnumMap<>(EvaluationRanker.class);
        Map<EvaluationRanker, Integer> emptyCounts = new EnumMap<>(EvaluationRanker.class);
        for (EvaluationRanker ranker : EvaluationRanker.values()) {
            coverage.put(ranker, new HashSet<>());
            emptyCounts.put(ranker, 0);
        }

        for (EvaluationUser user : dataset.users()) {
            for (EvaluationRanker ranker : EvaluationRanker.values()) {
                List<UUID> ranked = rank(jdbc, user.userId(), ranker);
                assertThat(ranked).doesNotContainAnyElementsOf(user.profileBookIds());
                EvaluationMetricValues metrics = RankingMetrics.at10(ranked, user.holdoutRelevantBookIds());
                perUser.add(new EvaluationReport.UserResult(user.userId(), ranker, ranked, metrics));
                coverage.get(ranker).addAll(ranked);
                if (ranked.isEmpty()) {
                    emptyCounts.put(ranker, emptyCounts.get(ranker) + 1);
                }
            }
        }

        List<EvaluationReport.AggregateResult> aggregates = aggregate(dataset, perUser, coverage, emptyCounts);
        EvaluationReport.RawResults rawResults = new EvaluationReport.RawResults(
                dataset.version(), dataset.seed(), K, List.copyOf(perUser), aggregates
        );
        EvaluationReport.RunMetadata metadata = metadata(dataset);

        Path output = Path.of(System.getProperty(
                "evaluation.outputDir",
                "services/recommendation-service/target/evaluation-results/reference"
        ));
        new EvaluationReportWriter().write(output, metadata, rawResults);

        assertThat(aggregates).hasSize(3);
        assertThat(output.resolve("run-metadata.json")).exists();
        assertThat(output.resolve("raw-results.json")).exists();
        assertThat(output.resolve("summary.md")).exists();
    }

    private static void persistDataset(
            NamedParameterJdbcTemplate jdbc,
            Dataset dataset,
            Map<UUID, EvaluationBook> booksById
    ) {
        for (EvaluationBook book : dataset.books()) {
            jdbc.update("""
                    INSERT INTO book_features (book_id, embedding, popularity_score, last_updated)
                    VALUES (:bookId, CAST(:embedding AS vector), :popularity, now())
                    """, new MapSqlParameterSource()
                    .addValue("bookId", book.bookId())
                    .addValue("embedding", vectorLiteral(book.embedding()))
                    .addValue("popularity", book.popularityScore()));
        }

        for (EvaluationUser user : dataset.users()) {
            float[] profileVector = SyntheticEvaluationDataset.buildProfileVector(user, booksById);
            jdbc.update("""
                    INSERT INTO user_profiles (
                        user_id, profile_vector, interacted_book_ids,
                        total_engagement_score, last_updated, created_at
                    )
                    VALUES (
                        :userId, CAST(:profileVector AS vector), CAST(:interacted AS uuid[]),
                        :engagement, now(), now()
                    )
                    """, new MapSqlParameterSource()
                    .addValue("userId", user.userId())
                    .addValue("profileVector", vectorLiteral(profileVector))
                    .addValue("interacted", uuidArrayLiteral(user.profileBookIds()))
                    .addValue("engagement", (double) user.profileBookIds().size()));
        }
    }

    private static List<UUID> rank(NamedParameterJdbcTemplate jdbc, UUID userId, EvaluationRanker ranker) {
        String sql = switch (ranker) {
            case POPULARITY_ONLY -> RankingQueries.POPULARITY_ONLY_FOR_USER;
            case SEMANTIC_ONLY -> RankingQueries.SEMANTIC_ONLY;
            case SEMANTIC_POPULARITY -> RankingQueries.SEMANTIC_POPULARITY;
        };

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", K)
                .addValue("offset", 0);

        return jdbc.query(sql, parameters, (resultSet, rowNumber) ->
                resultSet.getObject("book_id", UUID.class));
    }

    private static List<EvaluationReport.AggregateResult> aggregate(
            Dataset dataset,
            List<EvaluationReport.UserResult> perUser,
            Map<EvaluationRanker, Set<UUID>> coverage,
            Map<EvaluationRanker, Integer> emptyCounts
    ) {
        Map<EvaluationRanker, EvaluationMetricValues> means = new EnumMap<>(EvaluationRanker.class);
        for (EvaluationRanker ranker : EvaluationRanker.values()) {
            List<EvaluationMetricValues> values = perUser.stream()
                    .filter(result -> result.ranker() == ranker)
                    .map(EvaluationReport.UserResult::metrics)
                    .toList();
            means.put(ranker, mean(values));
        }

        EvaluationMetricValues baseline = means.get(EvaluationRanker.POPULARITY_ONLY);
        List<EvaluationReport.AggregateResult> aggregates = new ArrayList<>();
        for (EvaluationRanker ranker : EvaluationRanker.values()) {
            EvaluationMetricValues values = means.get(ranker);
            Map<String, EvaluationReport.Uplift> uplift = new LinkedHashMap<>();
            uplift.put("precisionAt10", uplift(values.precisionAt10(), baseline.precisionAt10()));
            uplift.put("recallAt10", uplift(values.recallAt10(), baseline.recallAt10()));
            uplift.put("ndcgAt10", uplift(values.ndcgAt10(), baseline.ndcgAt10()));
            uplift.put("mrrAt10", uplift(values.mrrAt10(), baseline.mrrAt10()));

            aggregates.add(new EvaluationReport.AggregateResult(
                    ranker,
                    values,
                    coverage.get(ranker).size() / (double) dataset.books().size(),
                    emptyCounts.get(ranker) / (double) dataset.users().size(),
                    Map.copyOf(uplift)
            ));
        }
        return List.copyOf(aggregates);
    }

    private static EvaluationMetricValues mean(List<EvaluationMetricValues> values) {
        return new EvaluationMetricValues(
                values.stream().mapToDouble(EvaluationMetricValues::precisionAt10).average().orElse(0.0),
                values.stream().mapToDouble(EvaluationMetricValues::recallAt10).average().orElse(0.0),
                values.stream().mapToDouble(EvaluationMetricValues::ndcgAt10).average().orElse(0.0),
                values.stream().mapToDouble(EvaluationMetricValues::mrrAt10).average().orElse(0.0)
        );
    }

    private static EvaluationReport.Uplift uplift(double current, double baseline) {
        double delta = current - baseline;
        Double relativeLift = baseline == 0.0 ? null : delta / baseline;
        return new EvaluationReport.Uplift(delta, relativeLift);
    }

    private static EvaluationReport.RunMetadata metadata(Dataset dataset) {
        String commitSha = System.getProperty(
                "evaluation.commitSha",
                System.getenv().getOrDefault("GITHUB_SHA", "local-uncommitted")
        );
        String command = System.getProperty(
                "evaluation.command",
                "mvn -pl services/recommendation-service -am -Dtest=RecommendationQualityEvaluationIT -Dgroups=evaluation test"
        );
        return new EvaluationReport.RunMetadata(
                "VellumHub",
                "issue-282-recommendation-quality",
                commitSha,
                dataset.version(),
                dataset.seed(),
                dataset.users().size(),
                dataset.books().size(),
                dataset.users().size() * SyntheticEvaluationDataset.PROFILE_INTERACTIONS_PER_USER,
                dataset.users().size() * SyntheticEvaluationDataset.HOLDOUT_RELEVANT_PER_USER,
                RankingQueries.CANDIDATE_POOL_SIZE,
                RankingQueries.SEMANTIC_WEIGHT,
                RankingQueries.POPULARITY_WEIGHT,
                0,
                1,
                System.getProperty("java.version"),
                System.getProperty("os.name") + " " + System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(),
                Instant.now().toString(),
                command,
                "Synthetic offline benchmark; binary relevance; no production SLA or online-impact claim."
        );
    }

    private static String vectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%.8f", vector[index]));
        }
        return builder.append(']').toString();
    }

    private static String uuidArrayLiteral(List<UUID> ids) {
        return "{" + ids.stream().map(UUID::toString).reduce((left, right) -> left + "," + right).orElse("") + "}";
    }
}

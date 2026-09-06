package com.vellumhub.recommendation_service.evaluation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EvaluationReport {

    private EvaluationReport() {
    }

    public record UserResult(
            UUID userId,
            EvaluationRanker ranker,
            List<UUID> rankedBookIds,
            EvaluationMetricValues metrics
    ) {
    }

    public record AggregateResult(
            EvaluationRanker ranker,
            EvaluationMetricValues metrics,
            double catalogCoverage,
            double emptyResultRate,
            Map<String, Uplift> upliftAgainstPopularity
    ) {
    }

    public record Uplift(double absoluteDelta, Double relativeLift) {
    }

    public record RawResults(
            String datasetVersion,
            long seed,
            int cutoff,
            List<UserResult> perUser,
            List<AggregateResult> aggregates
    ) {
    }

    public record RunMetadata(
            String project,
            String experimentId,
            String commitSha,
            String datasetVersion,
            long seed,
            String embeddingModel,
            String embeddingProvider,
            int users,
            int books,
            int profileInteractions,
            int holdoutInteractions,
            int candidatePoolSize,
            double semanticWeight,
            double popularityWeight,
            int warmupRuns,
            int repetitions,
            String javaVersion,
            String os,
            int availableProcessors,
            String timestampUtc,
            String executionCommand,
            String notes
    ) {
    }
}

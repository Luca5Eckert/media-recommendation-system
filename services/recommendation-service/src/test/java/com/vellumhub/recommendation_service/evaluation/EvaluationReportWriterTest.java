package com.vellumhub.recommendation_service.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationReportWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void serializesMachineReadableResultsAndMarkdownSummary() throws Exception {
        var metrics = new EvaluationMetricValues(0.5, 1.0, 0.9, 1.0);
        var aggregate = new EvaluationReport.AggregateResult(
                EvaluationRanker.SEMANTIC_POPULARITY,
                metrics,
                0.4,
                0.0,
                Map.of("ndcgAt10", new EvaluationReport.Uplift(0.2, 0.2857142857))
        );
        var raw = new EvaluationReport.RawResults(
                "fixture-v1",
                1L,
                10,
                List.of(new EvaluationReport.UserResult(UUID.randomUUID(), EvaluationRanker.SEMANTIC_POPULARITY, List.of(), metrics)),
                List.of(aggregate)
        );
        var metadata = new EvaluationReport.RunMetadata(
                "VellumHub", "recommendation-quality", "abc123", "fixture-v1", 1L,
                1, 1, 1, 1, 200, 0.7, 0.3, 0, 1,
                "21", "test-os", 1, "2026-09-06T00:00:00Z", "mvn test", "fixture"
        );

        new EvaluationReportWriter().write(tempDir, metadata, raw);

        assertThat(tempDir.resolve("run-metadata.json")).exists();
        assertThat(tempDir.resolve("raw-results.json")).exists();
        assertThat(tempDir.resolve("summary.md")).exists();
        assertThat(Files.readString(tempDir.resolve("summary.md")))
                .contains("nDCG@10", "semantic-popularity", "28.57%", "not a production SLA");
        assertThat(Files.readString(tempDir.resolve("raw-results.json")))
                .contains("SEMANTIC_POPULARITY", "ndcgAt10");
    }
}

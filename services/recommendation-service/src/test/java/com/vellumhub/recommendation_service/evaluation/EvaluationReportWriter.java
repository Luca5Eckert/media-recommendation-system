package com.vellumhub.recommendation_service.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class EvaluationReportWriter {

    private final ObjectMapper objectMapper;

    public EvaluationReportWriter() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void write(Path outputDirectory, EvaluationReport.RunMetadata metadata, EvaluationReport.RawResults results)
            throws IOException {
        Files.createDirectories(outputDirectory);
        objectMapper.writeValue(outputDirectory.resolve("run-metadata.json").toFile(), metadata);
        objectMapper.writeValue(outputDirectory.resolve("raw-results.json").toFile(), results);
        Files.writeString(outputDirectory.resolve("summary.md"), summary(metadata, results), StandardCharsets.UTF_8);
    }

    String summary(EvaluationReport.RunMetadata metadata, EvaluationReport.RawResults results) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# VellumHub Offline Recommendation Evaluation\n\n")
                .append("> Reproducible local/offline evaluation. These values are not a production SLA and do not represent online A/B-test evidence.\n\n")
                .append("Dataset: `").append(metadata.datasetVersion()).append("` (seed `")
                .append(metadata.seed()).append("`)  \n")
                .append("Commit: `").append(metadata.commitSha()).append("`  \n")
                .append("Embedding model: `").append(metadata.embeddingModel()).append("`  \n")
                .append("Embedding provider: `").append(metadata.embeddingProvider()).append("`  \n")
                .append("Users: ").append(metadata.users()).append(" · Books: ").append(metadata.books()).append("  \n")
                .append("Candidate pool: ").append(metadata.candidatePoolSize())
                .append(" · Combined weights: semantic ").append(format(metadata.semanticWeight()))
                .append(" / popularity ").append(format(metadata.popularityWeight())).append("\n\n")
                .append("| Ranker | Precision@10 | Recall@10 | nDCG@10 | MRR@10 | nDCG Δ vs popularity | nDCG lift | Catalog coverage | Empty rate |\n")
                .append("|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");

        for (EvaluationReport.AggregateResult aggregate : results.aggregates()) {
            EvaluationReport.Uplift ndcg = aggregate.upliftAgainstPopularity().get("ndcgAt10");
            markdown.append("| ").append(aggregate.ranker().id())
                    .append(" | ").append(format(aggregate.metrics().precisionAt10()))
                    .append(" | ").append(format(aggregate.metrics().recallAt10()))
                    .append(" | ").append(format(aggregate.metrics().ndcgAt10()))
                    .append(" | ").append(format(aggregate.metrics().mrrAt10()))
                    .append(" | ").append(format(ndcg.absoluteDelta()))
                    .append(" | ").append(ndcg.relativeLift() == null ? "n/a" : percent(ndcg.relativeLift()))
                    .append(" | ").append(percent(aggregate.catalogCoverage()))
                    .append(" | ").append(percent(aggregate.emptyResultRate()))
                    .append(" |\n");
        }

        markdown.append("\n## Protocol\n\n")
                .append("- Dataset records contain production-like text metadata, not hand-authored embedding vectors.\n")
                .append("- Every book vector is generated at runtime from title, author, genres, and description through the production embedding provider/model.\n")
                .append("- Profile vectors are built only from the profile/training split through the production `UserProfile` learning logic.\n")
                .append("- Holdout relevant items never participate in profile construction.\n")
                .append("- Already-interacted books are excluded for all three rankers.\n")
                .append("- The catalog is smaller than the production candidate pool so every eligible book is present before final ordering.\n")
                .append("- `semantic-popularity` executes the same canonical SQL used by the production repository.\n")
                .append("- `nDCG@10` is the primary comparison metric.\n")
                .append("\n## Reproduce\n\n```bash\n")
                .append(metadata.executionCommand()).append("\n```\n");

        return markdown.toString();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0);
    }
}

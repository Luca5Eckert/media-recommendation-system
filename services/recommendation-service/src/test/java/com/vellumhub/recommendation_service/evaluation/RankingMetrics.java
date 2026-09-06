package com.vellumhub.recommendation_service.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RankingMetrics {

    private RankingMetrics() {
    }

    public static EvaluationMetricValues at10(List<UUID> ranked, Set<UUID> relevant) {
        return new EvaluationMetricValues(
                precisionAtK(ranked, relevant, 10),
                recallAtK(ranked, relevant, 10),
                ndcgAtK(ranked, relevant, 10),
                mrrAtK(ranked, relevant, 10)
        );
    }

    public static double precisionAtK(List<UUID> ranked, Set<UUID> relevant, int k) {
        validate(k, relevant);
        long hits = topDistinct(ranked, k).stream().filter(relevant::contains).count();
        return (double) hits / k;
    }

    public static double recallAtK(List<UUID> ranked, Set<UUID> relevant, int k) {
        validate(k, relevant);
        if (relevant.isEmpty()) {
            return 0.0;
        }
        long hits = topDistinct(ranked, k).stream().filter(relevant::contains).count();
        return (double) hits / relevant.size();
    }

    public static double mrrAtK(List<UUID> ranked, Set<UUID> relevant, int k) {
        validate(k, relevant);
        List<UUID> top = topDistinct(ranked, k);
        for (int index = 0; index < top.size(); index++) {
            if (relevant.contains(top.get(index))) {
                return 1.0 / (index + 1);
            }
        }
        return 0.0;
    }

    public static double ndcgAtK(List<UUID> ranked, Set<UUID> relevant, int k) {
        validate(k, relevant);
        if (relevant.isEmpty()) {
            return 0.0;
        }

        List<UUID> top = topDistinct(ranked, k);
        double dcg = 0.0;
        for (int index = 0; index < top.size(); index++) {
            if (relevant.contains(top.get(index))) {
                dcg += 1.0 / log2(index + 2.0);
            }
        }

        int idealHits = Math.min(k, relevant.size());
        double idcg = 0.0;
        for (int index = 0; index < idealHits; index++) {
            idcg += 1.0 / log2(index + 2.0);
        }
        return idcg == 0.0 ? 0.0 : dcg / idcg;
    }

    private static List<UUID> topDistinct(List<UUID> ranked, int k) {
        Set<UUID> seen = new HashSet<>();
        return ranked.stream()
                .filter(seen::add)
                .limit(k)
                .toList();
    }

    private static void validate(int k, Set<UUID> relevant) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be greater than zero");
        }
        if (relevant == null) {
            throw new IllegalArgumentException("relevant set cannot be null");
        }
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2.0);
    }
}

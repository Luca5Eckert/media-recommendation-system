package com.vellumhub.recommendation_service.evaluation;

public record EvaluationMetricValues(
        double precisionAt10,
        double recallAt10,
        double ndcgAt10,
        double mrrAt10
) {
}

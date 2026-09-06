package com.vellumhub.recommendation_service.evaluation;

public enum EvaluationRanker {
    POPULARITY_ONLY("popularity-only"),
    SEMANTIC_ONLY("semantic-only"),
    SEMANTIC_POPULARITY("semantic-popularity");

    private final String id;

    EvaluationRanker(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}

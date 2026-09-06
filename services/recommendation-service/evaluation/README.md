# Offline Recommendation Quality Evaluation

This harness implements VellumHub issue #282. It compares the current personalized ranker with explicit ranking baselines using the same deterministic dataset, user profiles, holdout relevance set, exclusions, and eligible candidate pool.

## What is measured

Rankers:

1. `popularity-only`;
2. `semantic-only`;
3. `semantic-popularity` — the current production scoring query (`70%` semantic distance + `30%` popularity).

Primary metric: `nDCG@10`.

Required metrics:

- Precision@10;
- Recall@10;
- nDCG@10;
- MRR@10.

The summary also reports catalog coverage and empty-result rate.

## Dataset v1

`vellumhub-synthetic-v1` is generated deterministically with seed `2822026`.

- 120 books;
- 24 users;
- 6 latent preference clusters;
- 2 subtypes per cluster;
- 5 profile/training interactions per user;
- 5 holdout relevant items per user.

The holdout set never participates in profile construction. User profiles are built through the existing `UserProfile` vector-learning domain logic.

The v1 catalog is deliberately smaller than the production candidate pool of 200. After excluding the five profile interactions, every eligible book fits inside candidate generation. This means all three rankers see the same eligible items before final ordering while the combined ranker still executes the canonical production SQL.

## Run

Docker must be available because the reference evaluation uses `pgvector/pgvector:pg15` through Testcontainers and applies the real Flyway schema.

From the repository root:

```bash
mvn -pl services/recommendation-service -am \
  -Dtest=RecommendationQualityEvaluationIT \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dgroups=evaluation \
  test
```

`failIfNoSpecifiedTests=false` is required because `-am` first builds `kafka-contracts`, which intentionally does not contain the evaluation test selected for the recommendation-service.

A reference GitHub Actions run passes the PR head commit SHA and output location explicitly. Results are written to:

```text
services/recommendation-service/target/evaluation-results/reference/
  run-metadata.json
  raw-results.json
  summary.md
```

Regular unit/functional CI does not select `RecommendationQualityEvaluationIT`; deterministic metric, split and serialization tests remain part of the normal test suite.

## Interpretation guardrails

- This is offline evaluation, not evidence of online user impact.
- The dataset is synthetic and intentionally controlled; it is not production behavior.
- No quality threshold is used to make CI pass or fail.
- Do not tune the dataset after seeing results merely to make a ranker win.
- Do not call these values an SLA.
- Resume/portfolio claims must use only values from a versioned reference run and must identify them as reproducible offline evaluation when context requires it.

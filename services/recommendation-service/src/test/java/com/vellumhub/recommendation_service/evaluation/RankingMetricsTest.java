package com.vellumhub.recommendation_service.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RankingMetricsTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID D = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void calculatesPrecisionAndRecallAtK() {
        List<UUID> ranked = List.of(A, B, C, D);
        Set<UUID> relevant = Set.of(B, D);

        assertThat(RankingMetrics.precisionAtK(ranked, relevant, 4)).isEqualTo(0.5);
        assertThat(RankingMetrics.recallAtK(ranked, relevant, 4)).isEqualTo(1.0);
    }

    @Test
    void calculatesMrrFromFirstRelevantRank() {
        assertThat(RankingMetrics.mrrAtK(List.of(A, B, C), Set.of(B), 3)).isEqualTo(0.5);
        assertThat(RankingMetrics.mrrAtK(List.of(A, C), Set.of(B), 2)).isZero();
    }

    @Test
    void calculatesBinaryNdcgAgainstIdealOrdering() {
        double expected = (1.0 / (Math.log(3) / Math.log(2))) / 1.0;
        assertThat(RankingMetrics.ndcgAtK(List.of(A, B, C), Set.of(B), 3))
                .isCloseTo(expected, org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(RankingMetrics.ndcgAtK(List.of(B, A, C), Set.of(B), 3)).isEqualTo(1.0);
    }

    @Test
    void ignoresDuplicateRecommendationsWhenCalculatingMetrics() {
        assertThat(RankingMetrics.precisionAtK(List.of(B, B, A), Set.of(B), 2)).isEqualTo(0.5);
        assertThat(RankingMetrics.recallAtK(List.of(B, B, A), Set.of(B), 2)).isEqualTo(1.0);
    }
}

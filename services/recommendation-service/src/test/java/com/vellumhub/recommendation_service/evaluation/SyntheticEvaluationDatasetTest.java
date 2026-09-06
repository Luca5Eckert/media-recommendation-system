package com.vellumhub.recommendation_service.evaluation;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class SyntheticEvaluationDatasetTest {

    @Test
    void regeneratesTheSameVersionedDatasetDeterministically() {
        var first = SyntheticEvaluationDataset.generate();
        var second = SyntheticEvaluationDataset.generate();

        assertThat(first.version()).isEqualTo(SyntheticEvaluationDataset.VERSION);
        assertThat(first.seed()).isEqualTo(SyntheticEvaluationDataset.SEED);
        assertThat(first.books().stream().map(SyntheticEvaluationDataset.EvaluationBook::bookId).toList())
                .isEqualTo(second.books().stream().map(SyntheticEvaluationDataset.EvaluationBook::bookId).toList());
        assertThat(first.users()).isEqualTo(second.users());
        assertThat(first.books()).hasSize(SyntheticEvaluationDataset.BOOK_COUNT);
        assertThat(first.users()).hasSize(SyntheticEvaluationDataset.USER_COUNT);
    }

    @Test
    void profileInteractionsNeverLeakIntoHoldout() {
        var dataset = SyntheticEvaluationDataset.generate();

        for (var user : dataset.users()) {
            var overlap = new HashSet<>(user.profileBookIds());
            overlap.retainAll(user.holdoutRelevantBookIds());

            assertThat(overlap).isEmpty();
            assertThat(user.profileBookIds()).hasSize(SyntheticEvaluationDataset.PROFILE_INTERACTIONS_PER_USER);
            assertThat(user.holdoutRelevantBookIds()).hasSize(SyntheticEvaluationDataset.HOLDOUT_RELEVANT_PER_USER);
        }
    }

    @Test
    void buildsProfileOnlyFromTrainingInteractions() {
        var dataset = SyntheticEvaluationDataset.generate();
        var booksById = SyntheticEvaluationDataset.booksById(dataset);
        var user = dataset.users().getFirst();

        float[] profile = SyntheticEvaluationDataset.buildProfileVector(user, booksById);

        assertThat(profile).hasSize(384);
        double magnitude = 0.0;
        for (float value : profile) {
            magnitude += value * value;
        }
        assertThat(Math.sqrt(magnitude)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1.0e-5));
    }
}

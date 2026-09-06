package com.vellumhub.recommendation_service.evaluation;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SyntheticEvaluationDatasetTest {

    @Test
    void regeneratesTheSameVersionedDatasetDeterministically() {
        var first = SyntheticEvaluationDataset.generate();
        var second = SyntheticEvaluationDataset.generate();

        assertThat(first.version()).isEqualTo(SyntheticEvaluationDataset.VERSION);
        assertThat(first.seed()).isEqualTo(SyntheticEvaluationDataset.SEED);
        assertThat(first.books()).isEqualTo(second.books());
        assertThat(first.users()).isEqualTo(second.users());
        assertThat(first.books()).hasSize(SyntheticEvaluationDataset.BOOK_COUNT);
        assertThat(first.users()).hasSize(SyntheticEvaluationDataset.USER_COUNT);
    }

    @Test
    void booksExposeProductionLikeSemanticFieldsInsteadOfPrebuiltVectors() {
        var dataset = SyntheticEvaluationDataset.generate();

        assertThat(dataset.books()).allSatisfy(book -> {
            assertThat(book.title()).isNotBlank();
            assertThat(book.author()).isNotBlank();
            assertThat(book.description()).isNotBlank();
            assertThat(book.genres()).isNotEmpty();
            assertThat(book.popularityScore()).isBetween(0.10, 1.0);
        });
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
    void duplicateUsersForTheSamePreferenceUseComplementarySplits() {
        var dataset = SyntheticEvaluationDataset.generate();

        for (int index = 0; index < dataset.users().size(); index += 2) {
            var first = dataset.users().get(index);
            var second = dataset.users().get(index + 1);

            assertThat(first.preferredCluster()).isEqualTo(second.preferredCluster());
            assertThat(first.preferredSubtype()).isEqualTo(second.preferredSubtype());
            assertThat(first.profileBookIds()).containsExactlyInAnyOrderElementsOf(second.holdoutRelevantBookIds());
            assertThat(first.holdoutRelevantBookIds()).containsExactlyInAnyOrderElementsOf(second.profileBookIds());
        }
    }

    @Test
    void buildsProfileOnlyFromEmbeddingsGeneratedForTrainingInteractions() {
        var dataset = SyntheticEvaluationDataset.generate();
        var user = dataset.users().getFirst();
        Map<UUID, float[]> generatedEmbeddings = new LinkedHashMap<>();

        int dimension = 0;
        for (UUID bookId : user.profileBookIds()) {
            float[] embedding = new float[384];
            embedding[dimension++] = 1.0f;
            generatedEmbeddings.put(bookId, embedding);
        }

        float[] profile = SyntheticEvaluationDataset.buildProfileVector(user, generatedEmbeddings);

        assertThat(profile).hasSize(384);
        double magnitude = 0.0;
        for (float value : profile) {
            magnitude += value * value;
        }
        assertThat(Math.sqrt(magnitude)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1.0e-5));
    }
}

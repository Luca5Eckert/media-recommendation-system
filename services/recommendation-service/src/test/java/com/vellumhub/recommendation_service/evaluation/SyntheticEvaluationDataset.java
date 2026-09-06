package com.vellumhub.recommendation_service.evaluation;

import com.vellumhub.recommendation_service.module.user_profile.domain.model.ProfileAdjustment;
import com.vellumhub.recommendation_service.module.user_profile.domain.model.UserProfile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SyntheticEvaluationDataset {

    public static final String VERSION = "vellumhub-synthetic-v1";
    public static final long SEED = 282_2026L;
    public static final int BOOK_COUNT = 120;
    public static final int USER_COUNT = 24;
    public static final int PROFILE_INTERACTIONS_PER_USER = 5;
    public static final int HOLDOUT_RELEVANT_PER_USER = 5;

    private SyntheticEvaluationDataset() {
    }

    public static Dataset generate() {
        List<EvaluationBook> books = createBooks();
        Map<String, List<EvaluationBook>> booksByPreference = new LinkedHashMap<>();
        for (EvaluationBook book : books) {
            booksByPreference
                    .computeIfAbsent(key(book.cluster(), book.subtype()), ignored -> new ArrayList<>())
                    .add(book);
        }

        List<EvaluationUser> users = new ArrayList<>();
        for (int cluster = 0; cluster < 6; cluster++) {
            for (int subtype = 0; subtype < 2; subtype++) {
                List<EvaluationBook> preferred = booksByPreference.get(key(cluster, subtype));
                for (int duplicate = 0; duplicate < 2; duplicate++) {
                    UUID userId = stableUuid("user-" + cluster + "-" + subtype + "-" + duplicate);
                    List<UUID> profile = preferred.subList(0, PROFILE_INTERACTIONS_PER_USER).stream()
                            .map(EvaluationBook::bookId)
                            .toList();
                    Set<UUID> holdout = new LinkedHashSet<>(
                            preferred.subList(PROFILE_INTERACTIONS_PER_USER, preferred.size()).stream()
                                    .map(EvaluationBook::bookId)
                                    .toList()
                    );
                    users.add(new EvaluationUser(userId, cluster, subtype, profile, holdout));
                }
            }
        }

        return new Dataset(VERSION, SEED, List.copyOf(books), List.copyOf(users));
    }

    public static float[] buildProfileVector(EvaluationUser user, Map<UUID, EvaluationBook> booksById) {
        UserProfile profile = UserProfile.create(user.userId());
        for (UUID bookId : user.profileBookIds()) {
            EvaluationBook book = booksById.get(bookId);
            if (book == null) {
                throw new IllegalArgumentException("Unknown profile book: " + bookId);
            }
            profile.applyUpdate(ProfileAdjustment.of(bookId, 1.0f, book.embedding()));
        }
        return profile.getProfileVector().clone();
    }

    public static Map<UUID, EvaluationBook> booksById(Dataset dataset) {
        Map<UUID, EvaluationBook> result = new LinkedHashMap<>();
        for (EvaluationBook book : dataset.books()) {
            result.put(book.bookId(), book);
        }
        return result;
    }

    private static List<EvaluationBook> createBooks() {
        List<EvaluationBook> books = new ArrayList<>(BOOK_COUNT);
        int globalIndex = 0;
        for (int cluster = 0; cluster < 6; cluster++) {
            for (int subtype = 0; subtype < 2; subtype++) {
                for (int position = 0; position < 10; position++) {
                    UUID id = stableUuid("book-" + globalIndex);
                    float[] embedding = embedding(cluster, subtype, position);
                    double popularity = 1.0 - (globalIndex / (double) (BOOK_COUNT + 20));
                    books.add(new EvaluationBook(id, embedding, popularity, cluster, subtype));
                    globalIndex++;
                }
            }
        }
        return books;
    }

    private static float[] embedding(int cluster, int subtype, int position) {
        float[] vector = new float[384];
        vector[cluster] = 0.90f;
        vector[6 + (cluster * 2) + subtype] = 0.70f;
        vector[18 + ((cluster * 20 + subtype * 10 + position) % 366)] = 0.05f;

        double sumSquares = 0.0;
        for (float value : vector) {
            sumSquares += value * value;
        }
        float magnitude = (float) Math.sqrt(sumSquares);
        for (int index = 0; index < vector.length; index++) {
            vector[index] /= magnitude;
        }
        return vector;
    }

    private static UUID stableUuid(String value) {
        return UUID.nameUUIDFromBytes((VERSION + ":" + SEED + ":" + value)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String key(int cluster, int subtype) {
        return cluster + ":" + subtype;
    }

    public record Dataset(String version, long seed, List<EvaluationBook> books, List<EvaluationUser> users) {
    }

    public record EvaluationBook(UUID bookId, float[] embedding, double popularityScore, int cluster, int subtype) {
    }

    public record EvaluationUser(
            UUID userId,
            int preferredCluster,
            int preferredSubtype,
            List<UUID> profileBookIds,
            Set<UUID> holdoutRelevantBookIds
    ) {
    }
}

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
import java.util.SplittableRandom;
import java.util.UUID;

public final class SyntheticEvaluationDataset {

    public static final String VERSION = "vellumhub-synthetic-v2-model-embedded";
    public static final long SEED = 282_2026L;
    public static final int BOOK_COUNT = 120;
    public static final int USER_COUNT = 24;
    public static final int PROFILE_INTERACTIONS_PER_USER = 5;
    public static final int HOLDOUT_RELEVANT_PER_USER = 5;

    private static final List<String> TITLE_SUFFIXES = List.of(
            "Threshold", "Archive", "Signal", "Inheritance", "Crossing",
            "Pattern", "Promise", "Fault Line", "Aftermath", "Turning Point"
    );

    private static final List<String> AUTHORS = List.of(
            "Mara Bell", "Theo Grant", "Nina Clarke", "Julian Reyes",
            "Iris Morgan", "Samuel Vale", "Leah Chen", "Owen Hart",
            "Camila Brooks", "Jonas Reed", "Avery Cole", "Milo Bennett"
    );

    private static final List<String> BRIDGE_TOPICS = List.of(
            "connects personal decisions with institutional pressure",
            "uses an investigation to expose conflicting evidence",
            "shows how technology changes communication and trust",
            "places family loyalty beside professional ambition",
            "follows travel across unfamiliar places and communities",
            "examines competition, scarcity, and economic pressure",
            "develops a relationship alongside the central challenge",
            "uses inherited traditions and historical records to shape present choices",
            "returns to questions of identity, memory, and responsibility",
            "balances rapid events with careful analysis of long-term consequences"
    );

    private static final List<String> DETAIL_TOPICS = List.of(
            "uncertain alliances", "ethical trade-offs", "hidden incentives", "social status",
            "public reputation", "organizational failure", "competing interpretations",
            "resource constraints", "leadership under pressure", "unexpected cooperation"
    );

    private static final ThemeDefinition[] THEMES = {
            new ThemeDefinition(
                    "Ashen Crown", List.of("Fantasy", "Adventure"),
                    "A fragile alliance crosses rival kingdoms after an ancient relic returns to political life.",
                    "Magic, inheritance, military strategy, and the cost of leadership shape the journey."
            ),
            new ThemeDefinition(
                    "Hidden Borough", List.of("Fantasy", "Mystery"),
                    "A municipal investigator discovers that ordinary neighborhoods hide rival occult communities.",
                    "The case blends city life, supernatural rules, missing people, and questions of trust."
            ),
            new ThemeDefinition(
                    "Far Meridian", List.of("Science Fiction", "Adventure"),
                    "A starship crew carries diplomats and refugees between colonies during a fragile interplanetary ceasefire.",
                    "Alien cultures, navigation, political negotiation, and fleet conflict compete for the crew's attention."
            ),
            new ThemeDefinition(
                    "Neon Divide", List.of("Science Fiction", "Thriller"),
                    "In a corporate megacity, a security analyst uncovers a market built around stolen neural identities.",
                    "Surveillance, hacking, implants, organized crime, and corporate power drive the conflict."
            ),
            new ThemeDefinition(
                    "Glass Evidence", List.of("Mystery", "Crime"),
                    "A detective reopens a disappearance after a routine property dispute produces contradictory physical evidence.",
                    "Interviews, timelines, forensic details, motives, and procedural mistakes shape the investigation."
            ),
            new ThemeDefinition(
                    "Borrowed Memory", List.of("Thriller", "Mystery"),
                    "A witness with fragmented memories becomes central to a case involving a family secret and a staged accident.",
                    "An unreliable point of view, fear, obsession, manipulation, and psychological pressure complicate every clue."
            ),
            new ThemeDefinition(
                    "Second Platform", List.of("Romance", "Contemporary"),
                    "Two professionals repeatedly meet while their careers pull them toward different cities and priorities.",
                    "Friendship, work, communication, family expectations, and ordinary compromises shape the relationship."
            ),
            new ThemeDefinition(
                    "Letters at Winter", List.of("Romance", "Historical Fiction"),
                    "Two people from different social circles exchange letters while an estate and nearby town recover from war.",
                    "Courtship, class, duty, inheritance, reputation, and changing social expectations guide the relationship."
            ),
            new ThemeDefinition(
                    "River Kingdoms", List.of("History", "Nonfiction"),
                    "A nonfiction study compares ancient river civilizations through trade records, archaeology, and administrative texts.",
                    "The analysis focuses on state formation, agriculture, religion, logistics, warfare, and material evidence."
            ),
            new ThemeDefinition(
                    "Factory Century", List.of("History", "Nonfiction"),
                    "A nonfiction account traces industrialization through factories, railways, cities, firms, and labor movements.",
                    "It connects technological change with migration, productivity, working conditions, institutions, and social reform."
            ),
            new ThemeDefinition(
                    "Reliable Systems", List.of("Technology", "Nonfiction"),
                    "A practical software engineering book examines distributed systems, APIs, databases, queues, and service boundaries.",
                    "It emphasizes reliability, consistency, observability, testing, failure handling, and architectural trade-offs."
            ),
            new ThemeDefinition(
                    "Learning Machines", List.of("Technology", "Nonfiction"),
                    "A practical study of modern machine learning systems, from representation learning to language models.",
                    "It examines embeddings, evaluation, data quality, retrieval, model behavior, deployment, and responsible experimentation."
            )
    };

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
                    List<UUID> profile = new ArrayList<>(PROFILE_INTERACTIONS_PER_USER);
                    Set<UUID> holdout = new LinkedHashSet<>();

                    for (int position = 0; position < preferred.size(); position++) {
                        UUID bookId = preferred.get(position).bookId();
                        if ((position & 1) == duplicate) {
                            profile.add(bookId);
                        } else {
                            holdout.add(bookId);
                        }
                    }

                    users.add(new EvaluationUser(
                            userId,
                            cluster,
                            subtype,
                            List.copyOf(profile),
                            Set.copyOf(holdout)
                    ));
                }
            }
        }

        return new Dataset(VERSION, SEED, List.copyOf(books), List.copyOf(users));
    }

    public static float[] buildProfileVector(EvaluationUser user, Map<UUID, float[]> embeddingsByBookId) {
        UserProfile profile = UserProfile.create(user.userId());
        for (UUID bookId : user.profileBookIds()) {
            float[] embedding = embeddingsByBookId.get(bookId);
            if (embedding == null) {
                throw new IllegalArgumentException("Missing generated embedding for profile book: " + bookId);
            }
            profile.applyUpdate(ProfileAdjustment.of(bookId, 1.0f, embedding));
        }
        return profile.getProfileVector().clone();
    }

    private static List<EvaluationBook> createBooks() {
        List<EvaluationBook> books = new ArrayList<>(BOOK_COUNT);
        SplittableRandom popularity = new SplittableRandom(SEED);
        int globalIndex = 0;

        for (int cluster = 0; cluster < 6; cluster++) {
            for (int subtype = 0; subtype < 2; subtype++) {
                ThemeDefinition theme = theme(cluster, subtype);
                for (int position = 0; position < 10; position++) {
                    UUID id = stableUuid("book-" + globalIndex);
                    String title = theme.titleAnchor() + ": " + TITLE_SUFFIXES.get(position);
                    String author = AUTHORS.get(Math.floorMod(globalIndex * 7 + position * 3 + 5, AUTHORS.size()));
                    String description = theme.opening()
                            + " " + theme.focus()
                            + " The book also " + BRIDGE_TOPICS.get(Math.floorMod(position + cluster * 2 + subtype, BRIDGE_TOPICS.size()))
                            + " and explores " + DETAIL_TOPICS.get(Math.floorMod(position * 3 + cluster + subtype, DETAIL_TOPICS.size()))
                            + ".";
                    double popularityScore = 0.10 + (popularity.nextDouble() * 0.90);

                    books.add(new EvaluationBook(
                            id,
                            title,
                            author,
                            description,
                            theme.genres(),
                            popularityScore,
                            cluster,
                            subtype
                    ));
                    globalIndex++;
                }
            }
        }

        return books;
    }

    private static ThemeDefinition theme(int cluster, int subtype) {
        return THEMES[(cluster * 2) + subtype];
    }

    private static UUID stableUuid(String value) {
        return UUID.nameUUIDFromBytes((VERSION + ":" + SEED + ":" + value)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String key(int cluster, int subtype) {
        return cluster + ":" + subtype;
    }

    private record ThemeDefinition(
            String titleAnchor,
            List<String> genres,
            String opening,
            String focus
    ) {
    }

    public record Dataset(String version, long seed, List<EvaluationBook> books, List<EvaluationUser> users) {
    }

    public record EvaluationBook(
            UUID bookId,
            String title,
            String author,
            String description,
            List<String> genres,
            double popularityScore,
            int cluster,
            int subtype
    ) {
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

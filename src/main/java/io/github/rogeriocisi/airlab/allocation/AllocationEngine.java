package io.github.rogeriocisi.airlab.allocation;

import io.github.rogeriocisi.airlab.support.Hashes;
import io.github.rogeriocisi.airlab.support.Seeds;
import io.github.rogeriocisi.airlab.support.SeedStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The draw itself (Epic E3, Constitution P5).
 *
 * <p>Two properties matter more than anything else here:
 *
 * <ol>
 *   <li><b>The seed is unpredictable.</b> It comes from {@link SecureRandom}, 32 bytes.</li>
 *   <li><b>The draw is reproducible.</b> Given the same seed and the same input, this class
 *       returns the same plan on any machine, any JDK and any operating system. The expansion
 *       of the seed is {@link SeedStream} (SHA-256 in counter mode), and every list is sorted
 *       before use, so the order in which the application happened to load rows cannot change
 *       the result.</li>
 * </ol>
 *
 * <p>Each decision draws from its own domain string, so adding a decision later does not
 * shift the decisions made before it.
 */
public final class AllocationEngine {

    private AllocationEngine() {
    }

    /** A fresh 32 byte seed in lower case hex. Kept secret until the unblinding (P1, P5). */
    public static String newSeedHex() {
        return Seeds.newSeedHex();
    }

    /**
     * Reproduces the plan for a seed and an input. Pure function: no clock, no database,
     * no randomness beyond the seed.
     */
    public static AllocationPlan plan(String seedHex, DrawInput input) {
        byte[] seed = Hashes.fromHex(seedHex);

        List<DrawInput.PairInput> pairs = new ArrayList<>(input.pairs());
        pairs.sort(Comparator.comparing(DrawInput.PairInput::pairCode));

        List<String> mediums = new ArrayList<>(input.mediumCodes());
        mediums.sort(Comparator.naturalOrder());

        List<String> experimenters = new ArrayList<>(input.experimenterCodes());
        experimenters.sort(Comparator.naturalOrder());

        List<AllocationPlan.SessionAllocation> sessions = new ArrayList<>();
        List<AllocationPlan.PresentationOrder> presentations = new ArrayList<>();
        List<AllocationPlan.ConfirmatoryUnit> confirmatoryUnits = new ArrayList<>();
        List<AllocationPlan.SessionSlot> sessionSlots = new ArrayList<>();

        for (DrawInput.PairInput pair : pairs) {
            String medium = mediums.get(
                    new SeedStream(seed, "medium:" + pair.pairCode()).nextInt(mediums.size()));

            // Reading order inside the pair: which of the two targets the medium reads first.
            List<String> targets = new ArrayList<>(List.of(pair.targetA(), pair.targetB()));
            targets.sort(Comparator.naturalOrder());
            new SeedStream(seed, "reading-order:" + pair.pairCode()).shuffle(targets);

            SeedStream experimenterStream = new SeedStream(seed, "experimenter:" + pair.pairCode());
            for (int i = 0; i < targets.size(); i++) {
                String experimenter = experimenters.get(experimenterStream.nextInt(experimenters.size()));
                sessions.add(new AllocationPlan.SessionAllocation(
                        pair.pairCode(), targets.get(i), medium, experimenter, i + 1));
            }

            // Which sitter of this pair carries the one independent observation of the
            // confirmatory analysis. Drawn here, before any session, so that the unit of
            // analysis cannot be picked after the fact (SPEC, Epic E7).
            List<String> pairTargets = new ArrayList<>(List.of(pair.targetA(), pair.targetB()));
            pairTargets.sort(Comparator.naturalOrder());
            confirmatoryUnits.add(new AllocationPlan.ConfirmatoryUnit(
                    pair.pairCode(),
                    pairTargets.get(new SeedStream(seed, "confirmatory-unit:" + pair.pairCode())
                            .nextInt(pairTargets.size()))));

            // Presentation order, counterbalanced WITHIN the pair: one sitter sees their own
            // reading first and the other sees the decoy first. Beischel and Schwartz do it
            // this way; drawing it independently per sitter, as an earlier version did, allows
            // both sitters of a pair to get their own reading first, which wastes the balance
            // the pair design exists to provide. Which of the two gets own-first is still drawn,
            // so the order stays unpredictable (P1, P5, Epic E6).
            // Both sitters of the pair are shown the same reading first, so one of them sees
            // their own and the other sees the decoy. Which one is drawn; and the two targets
            // are put in canonical order first, so that listing a pair as (B, A) instead of
            // (A, B) cannot change the draw (P5).
            List<String> canonicalPair = new ArrayList<>(List.of(pair.targetA(), pair.targetB()));
            canonicalPair.sort(Comparator.naturalOrder());
            boolean firstOfPairSeesOwn =
                    new SeedStream(seed, "presentation:" + pair.pairCode()).nextBoolean();
            String shownFirst = firstOfPairSeesOwn ? canonicalPair.get(0) : canonicalPair.get(1);
            presentations.add(new AllocationPlan.PresentationOrder(canonicalPair.get(0), shownFirst));
            presentations.add(new AllocationPlan.PresentationOrder(canonicalPair.get(1), shownFirst));
        }

        sessions.sort(Comparator.comparing(AllocationPlan.SessionAllocation::pairCode)
                .thenComparing(AllocationPlan.SessionAllocation::readingOrder));

        sessionSlots.addAll(counterbalancedSlots(seed, sessions, input.sessionSlotsPerDay()));
        presentations.sort(Comparator.comparing(AllocationPlan.PresentationOrder::targetCode));
        confirmatoryUnits.sort(Comparator.comparing(AllocationPlan.ConfirmatoryUnit::pairCode));
        sessionSlots.sort(Comparator.comparing(AllocationPlan.SessionSlot::pairCode)
                .thenComparing(AllocationPlan.SessionSlot::targetCode));

        return new AllocationPlan(
                input.studyCode(), sessions, presentations, confirmatoryUnits, sessionSlots);
    }

    /**
     * Counterbalanced day and slot indices, so that no pair systematically occupies the early
     * or the late slot of a day (Epic E4, O'Keeffe and Wiseman's temporal-cue control).
     *
     * <p>Sessions are grouped by medium, because a medium reads in one sitting; within a
     * medium the sessions are laid out on a Latin square rotated by a seed-drawn offset. The
     * two readings of a pair therefore land in different slots, and which of them comes first
     * varies across pairs instead of following the reading order.
     */
    private static List<AllocationPlan.SessionSlot> counterbalancedSlots(
            byte[] seed, List<AllocationPlan.SessionAllocation> sessions, int slotsPerDay) {
        if (slotsPerDay < 1) {
            throw new IllegalArgumentException("a day needs at least one session slot");
        }
        Map<String, List<AllocationPlan.SessionAllocation>> byMedium = new TreeMap<>();
        for (AllocationPlan.SessionAllocation session : sessions) {
            byMedium.computeIfAbsent(session.mediumCode(), k -> new ArrayList<>()).add(session);
        }

        List<AllocationPlan.SessionSlot> slots = new ArrayList<>();
        for (Map.Entry<String, List<AllocationPlan.SessionAllocation>> entry : byMedium.entrySet()) {
            List<AllocationPlan.SessionAllocation> mediumSessions = new ArrayList<>(entry.getValue());
            mediumSessions.sort(Comparator.comparing(AllocationPlan.SessionAllocation::pairCode)
                    .thenComparing(AllocationPlan.SessionAllocation::targetCode));

            SeedStream stream = new SeedStream(seed, "schedule:" + entry.getKey());
            int offset = stream.nextInt(slotsPerDay);
            for (int i = 0; i < mediumSessions.size(); i++) {
                AllocationPlan.SessionAllocation session = mediumSessions.get(i);
                int day = i / slotsPerDay;
                // The rotation is what counterbalances: the same position in the sequence does
                // not always map to the same slot of the day.
                int slot = (i + offset + day) % slotsPerDay;
                slots.add(new AllocationPlan.SessionSlot(
                        session.pairCode(), session.targetCode(), day, slot));
            }
        }
        return slots;
    }

    /**
     * Order in which the items of a redacted reading are shown to the sitter (Epic E6).
     *
     * <p>O'Keeffe and Wiseman recommend separating the statements and randomly ordering them
     * before presentation, so that one revealing item cannot colour the sitter's reading of
     * the whole list. Derived from the seed, so the order is reproducible by an auditor once
     * the seed is revealed, and fixed per reading, so a sitter who reloads the page sees the
     * same list.
     *
     * @return a permutation of {@code 0..itemCount-1}
     */
    public static List<Integer> itemPresentationOrder(String seedHex, String redactedReadingCode,
                                                      int itemCount) {
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount cannot be negative");
        }
        List<Integer> order = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            order.add(i);
        }
        new SeedStream(Hashes.fromHex(seedHex), "item-order:" + redactedReadingCode).shuffle(order);
        return order;
    }

    /**
     * Which items of a reading to keep so that the two readings of a pair reach the sitter
     * with comparable length (Epic E5, Epic E6).
     *
     * <p>This is the other half of the length problem O'Keeffe and Wiseman's data exposed:
     * their readings ranged from 6 to 92 statements, and the longest, most general reading
     * drew the highest ratings from every sitter. Inside a pair, a reading with sixty items
     * against one with nine is a cue in itself, and a sum of ratings would reward the longer
     * one whether or not it was accurate. So the longer reading is cut to the agreed length by
     * a subsample drawn from the seed: reproducible, and not chosen by anyone who has seen
     * the content.
     *
     * @param keep how many items to keep; must not exceed {@code itemCount}
     * @return the indices to keep, in ascending order
     */
    public static List<Integer> balancedSubsample(String seedHex, String redactedReadingCode,
                                                  int itemCount, int keep) {
        if (keep < 0 || keep > itemCount) {
            throw new IllegalArgumentException("keep must be between 0 and itemCount");
        }
        List<Integer> candidates = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            candidates.add(i);
        }
        new SeedStream(Hashes.fromHex(seedHex), "length-balance:" + redactedReadingCode)
                .shuffle(candidates);
        List<Integer> kept = new ArrayList<>(candidates.subList(0, keep));
        kept.sort(Comparator.naturalOrder());
        return kept;
    }

    /**
     * P4 (SPEC E13, phase C): the commitment published in the frozen preregistration of a study
     * whose pairs form during collection, so that there is no plan to commit to yet. It covers the
     * seed and the canonical description of the study; every later draw is a public function of
     * this seed and of inputs recorded at the time of the draw.
     */
    public static String seedCommitment(String seedHex, String canonicalInput) {
        return Hashes.sha256Hex(seedHex + "|" + canonicalInput);
    }

    /**
     * P4: the medium who reads both requests of a pair, on a given attempt, among the eligible
     * mediums. The pool is sorted first, so the order in which accounts happened to be loaded
     * cannot change the result (P5); the attempt is part of the domain, so a resend after an
     * expired deadline is a new, independent draw.
     */
    public static String structuredMedium(String seedHex, String pairCode, int attempt,
                                          List<String> eligibleMediums) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempts start at 1");
        }
        List<String> pool = canonical(eligibleMediums);
        if (pool.isEmpty()) {
            throw new IllegalArgumentException("a draw needs at least one eligible medium");
        }
        int index = new SeedStream(Hashes.fromHex(seedHex), "p4-medium:" + pairCode + ":" + attempt)
                .nextInt(pool.size());
        return pool.get(index);
    }

    /**
     * P4: the order in which the medium sees the two requests of a pair, so that being shown
     * first is not a clue. Canonical order first, then a seed-drawn shuffle (P5).
     */
    public static List<String> structuredNameOrder(String seedHex, String pairCode, int attempt,
                                                   List<String> requestCodes) {
        List<String> order = canonical(requestCodes);
        new SeedStream(Hashes.fromHex(seedHex), "p4-name-order:" + pairCode + ":" + attempt).shuffle(order);
        return order;
    }

    /** Sorted copy; a repeated code is a bug upstream, never something to tolerate silently. */
    private static List<String> canonical(List<String> codes) {
        List<String> sorted = new ArrayList<>(codes);
        sorted.sort(Comparator.naturalOrder());
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1))) {
                throw new IllegalArgumentException("repeated code in a draw input: " + sorted.get(i));
            }
        }
        return sorted;
    }

    /**
     * The published commitment: {@code SHA-256(seedHex + "|" + canonical(plan))}.
     *
     * <p>Published before the first session; checked by anyone once the seed is revealed
     * (Constitution P5, step 2 and 3).
     */
    public static String commitment(String seedHex, AllocationPlan plan) {
        return Hashes.sha256Hex(seedHex + "|" + plan.canonicalForm());
    }
}

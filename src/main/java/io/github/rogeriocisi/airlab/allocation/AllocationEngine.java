package io.github.rogeriocisi.airlab.allocation;

import io.github.rogeriocisi.airlab.support.Hashes;
import io.github.rogeriocisi.airlab.support.SeedStream;
import java.security.SecureRandom;
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

    private static final SecureRandom SEED_SOURCE = new SecureRandom();

    private AllocationEngine() {
    }

    /** A fresh 32 byte seed in lower case hex. Kept secret until the unblinding (P1, P5). */
    public static String newSeedHex() {
        byte[] seed = new byte[32];
        SEED_SOURCE.nextBytes(seed);
        return Hashes.toHex(seed);
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
     * The published commitment: {@code SHA-256(seedHex + "|" + canonical(plan))}.
     *
     * <p>Published before the first session; checked by anyone once the seed is revealed
     * (Constitution P5, step 2 and 3).
     */
    public static String commitment(String seedHex, AllocationPlan plan) {
        return Hashes.sha256Hex(seedHex + "|" + plan.canonicalForm());
    }
}

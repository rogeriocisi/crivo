package io.github.rogeriocisi.airlab.allocation;

import io.github.rogeriocisi.airlab.support.Hashes;
import io.github.rogeriocisi.airlab.support.SeedStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

            // Presentation order to each sitter, drawn independently per sitter so that
            // "mine was shown first" carries no information (P1, Epic E6).
            for (String target : List.of(pair.targetA(), pair.targetB())) {
                String decoy = target.equals(pair.targetA()) ? pair.targetB() : pair.targetA();
                boolean ownFirst = new SeedStream(seed, "presentation:" + target).nextBoolean();
                presentations.add(new AllocationPlan.PresentationOrder(
                        target, ownFirst ? target : decoy));
            }
        }

        sessions.sort(Comparator.comparing(AllocationPlan.SessionAllocation::pairCode)
                .thenComparing(AllocationPlan.SessionAllocation::readingOrder));
        presentations.sort(Comparator.comparing(AllocationPlan.PresentationOrder::targetCode));
        confirmatoryUnits.sort(Comparator.comparing(AllocationPlan.ConfirmatoryUnit::pairCode));

        return new AllocationPlan(input.studyCode(), sessions, presentations, confirmatoryUnits);
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

package io.github.rogeriocisi.airlab.audit;

/**
 * Result of recomputing the audit chain.
 *
 * @param intact        true when every stored hash matches the recomputed one and every
 *                      event points at its predecessor
 * @param checkedEvents how many events were read
 * @param firstBrokenSeq sequence number of the first event that does not check out, or -1
 * @param detail        short, auditor facing explanation; never contains personal data
 */
public record ChainVerification(boolean intact, long checkedEvents, long firstBrokenSeq, String detail) {

    public static ChainVerification intact(long checkedEvents) {
        return new ChainVerification(true, checkedEvents, -1,
                "Cadeia intacta: " + checkedEvents + " evento(s) verificado(s).");
    }

    public static ChainVerification broken(long checkedEvents, long seq, String detail) {
        return new ChainVerification(false, checkedEvents, seq, detail);
    }
}

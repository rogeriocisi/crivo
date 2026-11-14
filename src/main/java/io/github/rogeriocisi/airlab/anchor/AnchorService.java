package io.github.rogeriocisi.airlab.anchor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * External anchoring of the audit trail (SPEC E8, v0.15).
 */
public interface AnchorService {

    /**
     * Stamps the head of the trail when it is due: a milestone happened since the last stamp, or the
     * trail changed and the last stamp is older than the interval. Never more than one request per
     * call, and none while backing off after a failure. The clock is a parameter so that the policy
     * can be tested; the server calls this every minute.
     */
    AnchorOutcome anchorIfDue(Instant now);

    /** Granted stamps, newest first. Public by construction: positions, hashes and times only. */
    List<AnchorView> granted();

    /** The DER {@code TimeStampResp} of a granted stamp, for {@code openssl ts -verify}. */
    Optional<byte[]> response(String code);

    /** Re-checks every granted stamp against the pinned certificates and the trail. */
    List<AnchorCheck> verifyAll();

    /** What the administrator panel and the public page say about the anchoring. */
    AnchorStatus status();
}

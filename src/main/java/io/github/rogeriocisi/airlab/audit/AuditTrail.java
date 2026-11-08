package io.github.rogeriocisi.airlab.audit;

import java.util.Map;

/**
 * Append-only, hash chained audit trail (Constitution P4).
 *
 * <p>The payload must contain pseudonymous codes, hashes, counts and enum values only.
 * Names, contacts, free text written by participants and anything else that could identify
 * a person never enter the trail (P6), because the trail is part of the public verification
 * package (P7).
 */
public interface AuditTrail {

    /**
     * Appends one event and returns its hash.
     *
     * @param payload domain detail, canonicalised before hashing; never personal data
     */
    String append(AuditEventType type,
                  ActorRole actorRole,
                  String actorCode,
                  String subjectRef,
                  Map<String, Object> payload);

    /** Recomputes the whole chain and reports the first break, if any. */
    ChainVerification verifyChain();

    /** Hash of the last event, or 64 zeros when the trail is empty. */
    String headHash();

    long size();
}

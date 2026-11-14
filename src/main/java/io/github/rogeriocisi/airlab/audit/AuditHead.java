package io.github.rogeriocisi.airlab.audit;

import java.time.Instant;

/**
 * The last event of the trail: its position in the chain and its hash. Stamping this hash at an
 * external authority attests the whole trail up to it, because every hash covers its predecessor
 * (SPEC E8, v0.15).
 *
 * @param position   sequence number of the event
 * @param hash       SHA-256 of the event, lower case hex
 * @param occurredAt when the event was recorded, as the platform says
 */
public record AuditHead(long position, String hash, Instant occurredAt) {
}

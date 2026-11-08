/**
 * Append-only audit trail, chained by hash (Constitution P4, Epic E8).
 *
 * <p>Every other module depends on this one, and this one depends on no domain module:
 * the trail must stay writable even when a feature is half built. Its entities live in
 * {@code internal} so that nothing outside can write a row by another path.
 */
@org.springframework.modulith.ApplicationModule(displayName = "audit")
package io.github.rogeriocisi.airlab.audit;

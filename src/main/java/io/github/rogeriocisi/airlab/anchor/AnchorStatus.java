package io.github.rogeriocisi.airlab.anchor;

import java.time.Instant;

/**
 * The state of the anchoring, for the administrator panel and the public page.
 *
 * @param enabled               whether the server stamps on its own schedule
 * @param authority             name of the configured authority
 * @param authorityUrl          where requests go
 * @param tsaCertificateSha256  pinned SHA-256 of the authority certificate file
 * @param caCertificateSha256   pinned SHA-256 of the root certificate file
 * @param grantedCount          stamps granted so far
 * @param lastAttestedAt        time attested by the last granted stamp, or null
 * @param failuresSinceLastGrant failed requests after the last granted stamp
 * @param lastFailureAt         when the last failed request was made, or null
 */
public record AnchorStatus(boolean enabled, String authority, String authorityUrl,
                           String tsaCertificateSha256, String caCertificateSha256,
                           long grantedCount, Instant lastAttestedAt,
                           long failuresSinceLastGrant, Instant lastFailureAt) {
}

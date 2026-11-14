package io.github.rogeriocisi.airlab.anchor;

import java.time.Instant;

/**
 * A granted stamp, as published: nothing here is personal or blinding sensitive, because the trail
 * itself carries only codes and hashes (P6, P7).
 *
 * @param code             pseudonymous code of the stamp, {@code ANC-…}
 * @param anchoredPosition position of the stamped event in the trail
 * @param anchoredHash     the stamped value: the hash of that event
 * @param attestedAt       the time the authority attests, not the platform's clock
 * @param authority        name of the authority, for example FreeTSA
 * @param serialHex        serial number of the token at the authority
 * @param responseSha256   SHA-256 of the stored {@code .tsr}, as it went to the trail
 */
public record AnchorView(String code, long anchoredPosition, String anchoredHash, Instant attestedAt,
                         String authority, String serialHex, String responseSha256) {
}

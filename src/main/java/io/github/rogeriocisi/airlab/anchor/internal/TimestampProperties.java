package io.github.rogeriocisi.airlab.anchor.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

/**
 * {@code airlab.timestamp.*}: the authority and the policy of the external anchoring (SPEC E8, v0.15).
 * The defaults are FreeTSA's, with the SHA-256 of each certificate file as FreeTSA publishes it.
 *
 * @param enabled              whether the server stamps on its own schedule (off in the tests)
 * @param authority            name shown on the public page
 * @param url                  RFC 3161 endpoint
 * @param tsaCertificate       the authority's signing certificate
 * @param tsaCertificateSha256 published SHA-256 of that file
 * @param caCertificate        the root that issued it
 * @param caCertificateSha256  published SHA-256 of that file
 * @param interval             a changed trail is stamped at least this often
 * @param retryAfter           wait after a failed request before trying again
 * @param timeout              connect and answer timeout of one request
 */
@ConfigurationProperties(prefix = "airlab.timestamp")
record TimestampProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("FreeTSA") String authority,
        @DefaultValue("https://freetsa.org/tsr") String url,
        @DefaultValue("classpath:timestamp/freetsa/tsa.crt") Resource tsaCertificate,
        @DefaultValue("8bfb0305bb64e2571ca507552ef3245cb1c2fee8728e0ff8689225081ea13467") String tsaCertificateSha256,
        @DefaultValue("classpath:timestamp/freetsa/cacert.pem") Resource caCertificate,
        @DefaultValue("2151b61137ffa86bf664691ba67e7da0b19f98c758e3d228d5d8ebf27e044438") String caCertificateSha256,
        @DefaultValue("PT24H") Duration interval,
        @DefaultValue("PT15M") Duration retryAfter,
        @DefaultValue("PT20S") Duration timeout) {
}

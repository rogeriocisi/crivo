package io.github.rogeriocisi.airlab.anchor.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rogeriocisi.airlab.support.Hashes;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.bouncycastle.tsp.TimeStampRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;

/**
 * One real round trip to FreeTSA, with the bundled certificates and their published pins. Off by
 * default, because the build must not depend on a third party being up: run it with
 * {@code AIRLAB_LIVE_TSA=true} before a release or after changing the authority settings.
 */
@EnabledIfEnvironmentVariable(named = "AIRLAB_LIVE_TSA", matches = "true")
class FreeTsaLiveTest {

    @Test
    @DisplayName("o FreeTSA carimba um hash aleatorio e a resposta passa por todas as conferencias")
    void freetsa_stamps_and_the_answer_checks_out() throws Exception {
        TrustAnchors trust = TrustAnchors.load(
                new ClassPathResource("timestamp/freetsa/tsa.crt"),
                "8bfb0305bb64e2571ca507552ef3245cb1c2fee8728e0ff8689225081ea13467",
                new ClassPathResource("timestamp/freetsa/cacert.pem"),
                "2151b61137ffa86bf664691ba67e7da0b19f98c758e3d228d5d8ebf27e044438");
        // A random value, not anything from a trail: this test stamps nothing of the project.
        byte[] value = Hashes.sha256(UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        TimeStampRequest request = TimestampProtocol.request(value);

        byte[] response = new HttpTsaClient("https://freetsa.org/tsr", Duration.ofSeconds(30))
                .exchange(request.getEncoded());
        TimestampProtocol.TokenInfo info = TimestampProtocol.checkResponse(response, request, trust);

        assertThat(Duration.between(info.attestedAt(), Instant.now()).abs()).isLessThan(Duration.ofMinutes(10));
        assertThat(TimestampProtocol.checkStored(response, value, trust).attestedAt()).isEqualTo(info.attestedAt());
    }
}

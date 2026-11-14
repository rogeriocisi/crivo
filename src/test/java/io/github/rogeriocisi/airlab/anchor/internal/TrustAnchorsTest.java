package io.github.rogeriocisi.airlab.anchor.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

/** The certificates a stamp is checked against are pinned by the SHA-256 the authority publishes. */
class TrustAnchorsTest {

    /** Published on https://freetsa.org/ next to each file (checked in November 2026). */
    private static final String FREETSA_TSA_SHA256 = "8bfb0305bb64e2571ca507552ef3245cb1c2fee8728e0ff8689225081ea13467";
    private static final String FREETSA_CA_SHA256 = "2151b61137ffa86bf664691ba67e7da0b19f98c758e3d228d5d8ebf27e044438";

    @Test
    @DisplayName("os certificados do FreeTSA no repositorio conferem com os valores publicados e formam cadeia")
    void the_bundled_freetsa_certificates_match_the_published_values() {
        TrustAnchors trust = TrustAnchors.load(
                new ClassPathResource("timestamp/freetsa/tsa.crt"), FREETSA_TSA_SHA256,
                new ClassPathResource("timestamp/freetsa/cacert.pem"), FREETSA_CA_SHA256);

        assertThat(trust.tsa().getSubjectX500Principal().getName()).contains("Free TSA");
        assertThat(trust.tsa().getIssuerX500Principal()).isEqualTo(trust.ca().getSubjectX500Principal());
    }

    @Test
    @DisplayName("um arquivo que nao confere com o SHA-256 fixado impede o uso")
    void a_file_that_does_not_match_its_pin_is_refused() {
        TestAuthority authority = TestAuthority.create("Fixacao");
        assertThatThrownBy(() -> TrustAnchors.load(
                new ByteArrayResource(authority.tsaPem()), "0".repeat(64),
                new ByteArrayResource(authority.caPem()), authority.caSha256()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHA-256 fixado");
    }

    @Test
    @DisplayName("certificado de carimbo emitido por outra raiz e recusado")
    void a_signing_certificate_from_another_root_is_refused() {
        TestAuthority one = TestAuthority.create("Uma");
        TestAuthority other = TestAuthority.create("Outra");
        assertThatThrownBy(() -> TrustAnchors.load(
                new ByteArrayResource(one.tsaPem()), one.tsaSha256(),
                new ByteArrayResource(other.caPem()), other.caSha256()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("raiz fixada");
    }

    @Test
    @DisplayName("certificado sem o uso critico de carimbo do tempo e recusado")
    void a_certificate_without_the_time_stamping_usage_is_refused() {
        TestAuthority plain = TestAuthority.create("Sem EKU", false);
        assertThatThrownBy(plain::trust)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("carimbo do tempo");
    }
}

package io.github.rogeriocisi.airlab.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Catalog rules of SPEC section 1.1, as tests. Rule 2 in particular — every protocol states
 * what its result does not support — is the kind of promise that quietly disappears from a
 * page unless something fails when it does.
 */
class ProtocolCatalogTest {

    @Test
    @DisplayName("o catalogo tem os quatro protocolos, com codigos e slugs unicos")
    void the_catalog_has_four_protocols() {
        List<Protocol> all = ProtocolCatalog.all();

        assertThat(all).hasSize(4);
        assertThat(all.stream().map(Protocol::code)).containsExactly("P1", "P2", "P3", "P4");
        assertThat(all.stream().map(Protocol::slug)).doesNotHaveDuplicates();
        assertThat(all.stream().map(Protocol::name)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("todo protocolo declara o que o resultado dele nao sustenta")
    void every_protocol_states_its_limits() {
        for (Protocol protocol : ProtocolCatalog.all()) {
            assertThat(protocol.limits())
                    .describedAs("limites de %s", protocol.code())
                    .isNotEmpty();
            protocol.limits().forEach(limit ->
                    assertThat(limit).describedAs("limite de %s", protocol.code())
                            .isNotBlank().hasSizeGreaterThan(20));
        }
    }

    @Test
    @DisplayName("um protocolo sem limites declarados nao pode ser construido")
    void a_protocol_without_limits_cannot_exist() {
        assertThatThrownBy(() -> new Protocol("P9", "x", "X", Protocol.Branch.ESP, 1, 4,
                Protocol.Automation.FULL, Protocol.Status.PLANNED, "resumo", List.of(), "E9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not support");
    }

    @Test
    @DisplayName("os testes de ESP avisam que nao dizem nada sobre mediunidade")
    void the_esp_protocols_say_they_are_not_about_mediumship() {
        for (Protocol protocol : List.of(ProtocolCatalog.precognitiveForcedChoice(),
                ProtocolCatalog.remoteViewing())) {
            assertThat(String.join(" ", protocol.limits()).toLowerCase())
                    .describedAs("limites de %s", protocol.code())
                    .contains("mediunidade");
        }
    }

    @Test
    @DisplayName("a leitura pareada avisa que nao distingue sobrevivencia de telepatia")
    void the_paired_reading_states_its_central_limit() {
        assertThat(String.join(" ", ProtocolCatalog.pairedReading().limits()).toLowerCase())
                .contains("telepatia");
    }

    @Test
    @DisplayName("o acaso declarado e uma probabilidade, e o percentual confere")
    void chance_is_a_declared_probability() {
        assertThat(ProtocolCatalog.pairedReading().chancePercent()).isEqualTo(50);
        assertThat(ProtocolCatalog.precognitiveForcedChoice().chancePercent()).isEqualTo(25);
        assertThat(ProtocolCatalog.remoteViewing().chancePercent()).isEqualTo(25);
        assertThat(ProtocolCatalog.structuredReading().chancePercent()).isEqualTo(50);

        assertThatThrownBy(() -> new Protocol("P9", "x", "X", Protocol.Branch.ESP, 4, 4,
                Protocol.Automation.FULL, Protocol.Status.PLANNED, "resumo",
                List.of("um limite declarado com tamanho suficiente"), "E9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chance");
    }

    @Test
    @DisplayName("as duas secoes cobrem o catalogo inteiro, com o AIR automatizado em destaque")
    void the_two_sections_cover_the_catalog() {
        assertThat(ProtocolCatalog.air().stream().map(Protocol::code)).containsExactly("P4", "P1");
        assertThat(ProtocolCatalog.extras().stream().map(Protocol::code)).containsExactly("P2", "P3");
        assertThat(ProtocolCatalog.air()).allMatch(p -> p.branch() == Protocol.Branch.AIR);
        assertThat(ProtocolCatalog.extras()).allMatch(p -> p.branch() == Protocol.Branch.ESP);
        assertThat(ProtocolCatalog.air().size() + ProtocolCatalog.extras().size())
                .isEqualTo(ProtocolCatalog.all().size());
    }

    @Test
    @DisplayName("o AIR automatizado declara a falta de proxy, o vazamento pelo nome e a telepatia")
    void the_structured_reading_states_its_limits() {
        String limits = String.join(" ", ProtocolCatalog.structuredReading().limits()).toLowerCase();
        assertThat(limits).contains("telepatia").contains("proxy").contains("primeiro nome")
                .contains("conluio");
        assertThat(ProtocolCatalog.structuredReading().automation()).isEqualTo(Protocol.Automation.FULL);
    }

    @Test
    @DisplayName("so os testes de ESP sao testes rapidos anonimos; o P4 exige conta")
    void only_esp_tests_are_quick_tests() {
        assertThat(ProtocolCatalog.all().stream().filter(Protocol::quickTest).map(Protocol::code))
                .containsExactly("P2", "P3");
    }

    @Test
    @DisplayName("busca por slug e por codigo")
    void lookup_works() {
        assertThat(ProtocolCatalog.bySlug("escolha-forcada")).contains(
                ProtocolCatalog.precognitiveForcedChoice());
        assertThat(ProtocolCatalog.byCode("P3")).contains(ProtocolCatalog.remoteViewing());
        assertThat(ProtocolCatalog.bySlug("nao-existe")).isEmpty();
    }
}

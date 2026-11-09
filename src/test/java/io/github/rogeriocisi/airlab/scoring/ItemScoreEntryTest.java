package io.github.rogeriocisi.airlab.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Epic E6: the item scale keeps the categories where cold reading and the Barnum effect live,
 * and makes the sitter write down what they meant when they use them.
 */
class ItemScoreEntryTest {

    @Test
    @DisplayName("encaixe obvio nao pede explicacao")
    void an_obvious_fit_needs_no_explanation() {
        ItemScoreEntry entry = ItemScoreEntry.of(1L, ItemScoreValue.OBVIOUS_FIT);

        assertThat(entry.justification()).isNull();
        assertThat(entry.value().countsAsFit()).isTrue();
    }

    @Test
    @DisplayName("encaixe interpretado sem explicacao e recusado")
    void an_interpreted_fit_without_an_explanation_is_refused() {
        assertThatThrownBy(() ->
                new ItemScoreEntry(1L, ItemScoreValue.PARTIAL_OR_INTERPRETED_FIT, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("explicacao");
    }

    @Test
    @DisplayName("encaixe em outra pessoa exige explicacao e nao conta como acerto")
    void a_fit_to_someone_else_is_explained_and_does_not_count() {
        ItemScoreEntry entry = new ItemScoreEntry(2L, ItemScoreValue.FITS_SOMEONE_ELSE,
                "Isso descreve meu tio, nao a pessoa de quem falo");

        assertThat(entry.value().countsAsFit()).isFalse();
        assertThat(entry.justification()).isNotBlank();
    }

    @Test
    @DisplayName("explicacao escrita em valor que nao admite explicacao e recusada")
    void an_explanation_where_none_belongs_is_refused() {
        assertThatThrownBy(() ->
                new ItemScoreEntry(3L, ItemScoreValue.NO_FIT, "nao reconheco nada disso"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao admite");
    }

    @Test
    @DisplayName("\"nao sei avaliar\" e contado, nao tratado como erro")
    void cannot_assess_is_its_own_category() {
        ItemScoreEntry entry = ItemScoreEntry.of(4L, ItemScoreValue.CANNOT_ASSESS);

        assertThat(entry.value().countsAsFit()).isFalse();
        assertThat(entry.value()).isNotEqualTo(ItemScoreValue.NO_FIT);
    }

    @Test
    @DisplayName("so o encaixe obvio conta para a acuracia da medida primaria")
    void only_an_obvious_fit_counts_towards_accuracy() {
        for (ItemScoreValue value : ItemScoreValue.values()) {
            assertThat(value.countsAsFit())
                    .describedAs("%s", value)
                    .isEqualTo(value == ItemScoreValue.OBVIOUS_FIT);
        }
    }
}

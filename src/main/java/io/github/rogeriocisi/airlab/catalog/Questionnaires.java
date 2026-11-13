package io.github.rogeriocisi.airlab.catalog;

import io.github.rogeriocisi.airlab.catalog.Questionnaire.Field;
import io.github.rogeriocisi.airlab.catalog.Questionnaire.Option;
import io.github.rogeriocisi.airlab.catalog.Questionnaire.Use;
import java.util.List;
import java.util.Optional;

/**
 * The questionnaires of P4, by version (SPEC E13, annex).
 *
 * <p>A constant, like the catalog: a questionnaire changes only with a new version, and the old
 * one stays here, because requests answered under it must still be readable and verifiable.
 *
 * <p>Version {@code v1-rascunho} is the draft of the SPEC annex, awaiting the researcher's
 * review. The pairing bands are the same as the P1 ones ({@code TargetRegistration}), so that
 * pairing is the same rule in both protocols.
 */
public final class Questionnaires {

    public static final String GENDER = "genero";
    public static final String AGE_BAND = "faixa-etaria";
    public static final String DEATH_PERIOD = "periodo-falecimento";

    private static final String NAME_GIVES_IT_AWAY =
            "Fica fora da nota: o primeiro nome costuma entregar o gênero, e ele é critério de pareamento.";
    private static final String EQUAL_IN_PAIR =
            "Fica fora da nota: é critério de pareamento, igual nos dois pedidos do par, e não distingue um do outro.";

    private static final Questionnaire V1_DRAFT = new Questionnaire("v1-rascunho", List.of(
            pairing(GENDER, "Gênero", NAME_GIVES_IT_AWAY,
                    "feminino", "Feminino",
                    "masculino", "Masculino",
                    "outro", "Outro"),
            pairing(AGE_BAND, "Idade ao falecer", EQUAL_IN_PAIR,
                    "ate-17", "Até 17 anos",
                    "18-39", "De 18 a 39 anos",
                    "40-59", "De 40 a 59 anos",
                    "60-79", "De 60 a 79 anos",
                    "80-mais", "80 anos ou mais"),
            pairing(DEATH_PERIOD, "Há quanto tempo faleceu", EQUAL_IN_PAIR,
                    "menos-1-ano", "Menos de 1 ano",
                    "1-5-anos", "De 1 a 5 anos",
                    "5-15-anos", "De 5 a 15 anos",
                    "mais-15-anos", "Mais de 15 anos"),
            scored("como-faleceu", "Como foi a morte",
                    "subita", "De forma súbita",
                    "prolongada", "Após doença prolongada"),
            scored("causa", "Causa principal",
                    "coracao", "Coração ou circulação",
                    "cancer", "Câncer",
                    "respiratoria", "Doença respiratória",
                    "acidente", "Acidente",
                    "violencia", "Violência",
                    "outra-doenca", "Outra doença"),
            scored("local-falecimento", "Onde faleceu",
                    "hospital", "No hospital",
                    "casa", "Em casa",
                    "rua-transito", "Na rua ou no trânsito",
                    "outro", "Em outro lugar"),
            scored("estado-civil", "Estado civil",
                    "solteiro", "Solteiro(a)",
                    "casado", "Casado(a) ou em união",
                    "viuvo", "Viúvo(a)",
                    "separado", "Separado(a)"),
            scored("filhos", "Tinha filhos",
                    "sim", "Sim",
                    "nao", "Não"),
            scored("ocupacao", "Ocupação principal",
                    "campo", "Trabalho no campo",
                    "oficio", "Ofício manual ou indústria",
                    "comercio", "Comércio",
                    "escritorio", "Escritório ou serviços",
                    "saude", "Saúde",
                    "educacao", "Educação",
                    "casa", "Cuidou da casa",
                    "outra", "Outra"),
            scored("onde-viveu", "Onde viveu a maior parte da vida",
                    "cidade-grande", "Cidade grande",
                    "cidade-media-pequena", "Cidade média ou pequena",
                    "rural", "Zona rural"),
            scored("temperamento", "Temperamento",
                    "expansivo", "Mais expansivo",
                    "reservado", "Mais reservado"),
            scored("estatura", "Estatura",
                    "abaixo", "Abaixo da média",
                    "media", "Na média",
                    "acima", "Acima da média"),
            scored("religiosidade", "Religiosidade",
                    "praticante", "Praticante",
                    "nao-praticante", "Não praticante",
                    "sem-religiao", "Sem religião"),
            scored("passatempo", "Passatempo mais marcante",
                    "musica", "Música",
                    "trabalhos-manuais", "Trabalhos manuais",
                    "esporte", "Esporte",
                    "leitura", "Leitura",
                    "cozinha", "Cozinha",
                    "plantas", "Plantas",
                    "outro", "Outro"),
            scored("oculos", "Usava óculos",
                    "sim", "Sim",
                    "nao", "Não"),
            scored("fumava", "Fumava",
                    "sim", "Sim",
                    "nao", "Não")));

    private static final List<Questionnaire> ALL = List.of(V1_DRAFT);

    private Questionnaires() {
    }

    /** The version new requests are answered under. */
    public static Questionnaire current() {
        return V1_DRAFT;
    }

    public static Optional<Questionnaire> byVersion(String version) {
        return ALL.stream().filter(questionnaire -> questionnaire.version().equals(version)).findFirst();
    }

    private static Field scored(String key, String label, String... keysAndLabels) {
        return new Field(key, label, Use.SCORED, null, options(keysAndLabels));
    }

    private static Field pairing(String key, String label, String reason, String... keysAndLabels) {
        return new Field(key, label, Use.PAIRING, reason, options(keysAndLabels));
    }

    private static List<Option> options(String... keysAndLabels) {
        if (keysAndLabels.length % 2 != 0) {
            throw new IllegalArgumentException("options come in key and label pairs");
        }
        Option[] out = new Option[keysAndLabels.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Option(keysAndLabels[2 * i], keysAndLabels[2 * i + 1]);
        }
        return List.of(out);
    }
}

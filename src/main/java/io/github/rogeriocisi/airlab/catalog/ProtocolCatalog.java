package io.github.rogeriocisi.airlab.catalog;

import java.util.List;
import java.util.Optional;

/**
 * The protocol catalog of the platform (SPEC, section 1.1).
 *
 * <p>Deliberately a constant, not a table: the catalog is part of the specification, changes
 * only with a new SPEC version, and must read the same in the application, in the exported
 * verification package and in the repository.
 *
 * <p>The text is in Portuguese because it is interface, and it is written out in full, with
 * accents, because it is rendered to people as it stands here.
 */
public final class ProtocolCatalog {

    private static final Protocol PAIRED_READING = new Protocol(
            "P1",
            "leitura-pareada",
            "Leitura pareada cega",
            Protocol.Branch.AIR,
            1, 2,
            Protocol.Automation.PARTIAL,
            Protocol.Status.IMPLEMENTED,
            "O médium produz uma leitura sobre um falecido-alvo conhecendo apenas o primeiro nome, "
                    + "entregue por um experimentador que não sabe nada sobre essa pessoa. A leitura é "
                    + "convertida em itens verificáveis por quem não sabe de quem ela é. O consulente "
                    + "recebe duas leituras, a do alvo dele e a do chamariz, sem rótulo, pontua os "
                    + "itens e escolhe qual acredita ser a sua.",
            List.of(
                    "Não distingue sobrevivência de telepatia com pessoas vivas: um acerto acima do "
                            + "acaso diz que houve informação, não de onde ela veio.",
                    "Não é automatizável por inteiro. A sessão permanece síncrona, com experimentador "
                            + "proxy, porque nos dois estudos em que o médium não interagiu com ninguém o "
                            + "resultado foi nulo (Silva e Moreira-Almeida, 2022).",
                    "Depende de julgamento humano na pontuação, e por isso a escala entra congelada no "
                            + "pré-registro."),
            "E1 a E9");

    private static final Protocol PRECOGNITIVE_FORCED_CHOICE = new Protocol(
            "P2",
            "escolha-forcada",
            "Escolha forçada precognitiva",
            Protocol.Branch.ESP,
            1, 4,
            Protocol.Automation.FULL,
            Protocol.Status.IMPLEMENTED,
            "A cada tentativa a pessoa escolhe entre quatro alternativas. O alvo daquela tentativa é "
                    + "derivado de uma semente sorteada no início da sessão e só é calculado depois que "
                    + "a resposta está travada. No fim da sessão a semente é revelada, e qualquer pessoa "
                    + "recomputa todos os alvos e confere que nenhum deles foi escolhido depois da "
                    + "resposta.",
            List.of(
                    "Não diz nada sobre falecidos nem sobre mediunidade: é um teste de precognição.",
                    "As replicações grandes dos achados de Bem deram nulo (Ritchie, Wiseman e French, "
                            + "2012; Galak et al., 2012). A expectativa razoável para este teste é "
                            + "resultado nulo.",
                    "O conjunto público é exploratório, nunca confirmatório: não se sabe quem testou, "
                            + "quantas vezes nem em que condições."),
            "E11");

    private static final Protocol REMOTE_VIEWING = new Protocol(
            "P3",
            "visao-remota",
            "Visão remota com quatro alvos",
            Protocol.Branch.ESP,
            1, 4,
            Protocol.Automation.FULL,
            Protocol.Status.IMPLEMENTED,
            "O sistema sorteia uma figura-alvo de um banco versionado. A pessoa escreve a impressão "
                    + "dela antes de ver qualquer candidata, e essa impressão é travada. Só então "
                    + "aparecem quatro figuras, o alvo e três chamarizes, para ela ordenar da mais "
                    + "parecida com a impressão à menos parecida. Acerto é o alvo em primeiro lugar.",
            List.of(
                    "Não diz nada sobre falecidos nem sobre mediunidade.",
                    "Quem julga a correspondência é a própria pessoa, o que reintroduz a crítica de "
                            + "\"como se pontua\" que a escolha forçada não tem. É o preço do formato de "
                            + "resposta livre.",
                    "O conjunto público é exploratório, pelas mesmas razões do P2."),
            "E12");

    private static final Protocol STRUCTURED_READING = new Protocol(
            "P4",
            "leitura-estruturada",
            "Leitura estruturada automatizada",
            Protocol.Branch.AIR,
            1, 2,
            Protocol.Automation.FULL,
            Protocol.Status.IN_PROGRESS,
            "O consulente cria uma conta, informa o primeiro nome do falecido e responde a um "
                    + "questionário fechado sobre ele; as respostas são travadas antes de qualquer "
                    + "médium receber o pedido. O médium recebe só o primeiro nome e responde ao mesmo "
                    + "questionário. O sistema confere as respostas campo a campo contra o alvo "
                    + "verdadeiro e contra um chamariz de perfil semelhante, e ninguém julga se uma "
                    + "afirmação encaixa. Tudo acontece pelo site, sem ninguém da equipe no circuito.",
            List.of(
                    "Não distingue sobrevivência de telepatia com pessoas vivas: as respostas "
                            + "existem na memória do consulente e no banco de dados.",
                    "Não tem experimentador proxy. Nos dois estudos em que o médium não interagiu "
                            + "com ninguém o resultado foi nulo (Silva e Moreira-Almeida, 2022), e um "
                            + "nulo aqui não refuta a leitura pareada.",
                    "O questionário fechado só captura o que cabe nos campos: as afirmações livres e "
                            + "específicas, que costumam ser as mais marcantes, ficam fora da nota.",
                    "O primeiro nome carrega informação, como o gênero e às vezes a geração. O "
                            + "pareamento e os campos fora da nota reduzem esse vazamento, mas não o "
                            + "eliminam.",
                    "Com inscrição aberta, o conluio entre médium e consulente que se conhecem e usam "
                            + "e-mails diferentes não é detectável com certeza."),
            "E13");

    private static final List<Protocol> ALL =
            List.of(PAIRED_READING, PRECOGNITIVE_FORCED_CHOICE, REMOTE_VIEWING, STRUCTURED_READING);

    /** The AIR section of the catalog, automated protocol first: it is the one featured. */
    private static final List<Protocol> AIR = List.of(STRUCTURED_READING, PAIRED_READING);

    /** The extra experiments: the ESP tests, which say nothing about mediumship. */
    private static final List<Protocol> EXTRAS = List.of(PRECOGNITIVE_FORCED_CHOICE, REMOTE_VIEWING);

    private ProtocolCatalog() {
    }

    /** Every protocol, in code order. */
    public static List<Protocol> all() {
        return ALL;
    }

    /** AIR protocols, in the order the site presents them. */
    public static List<Protocol> air() {
        return AIR;
    }

    /** Extra experiments, in the order the site presents them. */
    public static List<Protocol> extras() {
        return EXTRAS;
    }

    public static Protocol structuredReading() {
        return STRUCTURED_READING;
    }

    public static Optional<Protocol> bySlug(String slug) {
        return ALL.stream().filter(protocol -> protocol.slug().equals(slug)).findFirst();
    }

    public static Optional<Protocol> byCode(String code) {
        return ALL.stream().filter(protocol -> protocol.code().equals(code)).findFirst();
    }

    public static Protocol pairedReading() {
        return PAIRED_READING;
    }

    public static Protocol precognitiveForcedChoice() {
        return PRECOGNITIVE_FORCED_CHOICE;
    }

    public static Protocol remoteViewing() {
        return REMOTE_VIEWING;
    }
}

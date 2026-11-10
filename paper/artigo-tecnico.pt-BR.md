---
title: "Crivo: uma plataforma de código aberto para estudos de mediunidade cegos, pré-registrados e auditáveis"
subtitle: "Arquitetura de software para pesquisa de recepção anômala de informação (AIR)"
author:
  - name: Rogerio Cisi
    affiliation: "TODO: afiliação (pesquisador independente?)"
lang: pt-BR
date: "Rascunho v0.1 — novembro de 2026"
bibliography: referencias.bib
keywords: [mediunidade, recepção anômala de informação, protocolo cego, pré-registro, reprodutibilidade, auditoria, software livre]
status: rascunho
---

<!--
NOTAS DE REDAÇÃO (remover antes de submeter)
- Tipo de artigo: artigo de desenho/arquitetura (design paper). Ainda não há implementação,
  então nenhuma afirmação de desempenho ou resultado empírico deve aparecer no texto.
- Citações no formato Pandoc [@chave]; chaves em referencias.bib.
- Números dos estudos citados vêm de resumos e fontes secundárias: conferir no texto integral.
- Marcadores TODO indicam o que falta.
- Para gerar PDF: pandoc artigo-tecnico.pt-BR.md --citeproc -o artigo.pdf
-->

# Resumo

A pesquisa experimental sobre mediunidade tem como problema central a credibilidade do método: demonstrar que a informação produzida por um médium não chegou até ele por vias convencionais, como pistas sensoriais, leitura fria, pesquisa prévia, viés de avaliação ou fraude. Protocolos com múltiplas camadas de cegamento, alvos pareados com chamarizes e avaliação cega já foram descritos na literatura de recepção anômala de informação (AIR). A dificuldade de executá-los, porém, é sobretudo logística: controlar quem sabe o quê e em que momento, e conseguir provar isso depois a terceiros. Este artigo apresenta o desenho do **Crivo**, uma plataforma de código aberto que trata esses requisitos metodológicos como **invariantes de software**. O Crivo combina quatro mecanismos:

- uma matriz de visibilidade por papel, imposta na camada de dados e verificada por testes automatizados;
- sorteio criptograficamente seguro com esquema de compromisso e revelação;
- pré-registro congelado por hash e carimbo de tempo antes da coleta;
- uma trilha de auditoria somente de acréscimo, encadeada por hash, verificável por uma ferramenta independente.

A plataforma é deliberadamente neutra quanto ao resultado: nulos e positivos produzem o mesmo relatório e o mesmo pacote de verificação. O artigo descreve os requisitos derivados da literatura, o protocolo implementado na primeira versão, a arquitetura, o modelo de ameaças com as garantias e os limites do que o software pode assegurar, e as considerações éticas e de proteção de dados (LGPD).

**Palavras-chave:** mediunidade; recepção anômala de informação; protocolo cego; pré-registro; reprodutibilidade; auditoria; software livre.

<!-- TODO: Abstract em inglês, quando houver versão para submissão internacional. -->

# 1. Introdução

Relatos de médiuns que fornecem informações corretas sobre pessoas falecidas são abundantes na cultura brasileira e na literatura espírita. A pergunta científica, porém, não é se tais relatos existem, e sim se, **sob condições que excluam as explicações convencionais**, a informação produzida excede o que se esperaria por acaso.

A literatura experimental chama esse fenômeno de *anomalous information reception* (AIR), ou recepção anômala de informação. O termo é propositalmente neutro quanto à origem: descreve a chegada de informação por via não convencional sem afirmar se ela provém da sobrevivência da consciência ou de capacidades psíquicas dos vivos [@beischel2007]. Estudos com cegamento múltiplo relataram desempenho acima do acaso [@beischel2007; @beischel2015]. Outros, com metodologia própria, não encontraram suporte para a capacidade alegada [@okeeffe2005]. Revisões recentes atribuem parte da divergência a diferenças no controle de vazamento de informação e no desenho dos protocolos [@silva2022].

Esse quadro sugere que o gargalo da área é menos a teoria e mais a **execução verificável** de protocolos. Um estudo cego depende de que dezenas de pequenas regras sejam cumpridas: o experimentador não pode conhecer o falecido; o consulente não pode saber qual leitura é a sua; o sorteio não pode ser refeito depois de visto. O cumprimento dessas regras costuma ficar registrado, quando fica, em procedimentos manuais difíceis de auditar. Outra limitação reconhecida é a concentração dos dados em poucos laboratórios [@beischel2015], o que torna a replicação independente especialmente valiosa.

Este artigo propõe que esses requisitos sejam tratados como **propriedades de um sistema de software**, implementadas e testadas da mesma forma que se testam requisitos de segurança. As contribuições são:

1. um conjunto de **requisitos derivados da literatura**, organizados como um modelo de ameaças de vazamento de informação (Seção 3);
2. a especificação de um **protocolo pareado** com cegamento múltiplo expresso como matriz de visibilidade testável (Seção 4);
3. uma **arquitetura** que combina projeções de dados por papel, sorteio com compromisso e revelação, pré-registro com carimbo de tempo e trilha de auditoria encadeada por hash (Seção 5);
4. uma análise explícita das **garantias e limites** do que o software pode e não pode assegurar (Seção 6).

O Crivo está em fase de especificação. Este texto descreve o desenho, não resultados empíricos.

# 2. Trabalhos relacionados

## 2.1 Protocolos de AIR com cegamento

Beischel e Schwartz [-@beischel2007] descreveram um protocolo com cegamento triplo. Os consulentes ficaram ausentes e foram representados por um experimentador que desconhecia as identidades (*proxy consulente*). Cada falecido foi pareado com outro do mesmo gênero, e cada consulente pontuou duas transcrições itemizadas, a sua e a de controle, antes de escolher a que mais se ajustava. A replicação e extensão posterior [@beischel2015] envolveu 20 médiuns e 86 leituras por telefone, com o médium recebendo apenas o primeiro nome do falecido. Os autores relataram leituras-alvo avaliadas como mais precisas que os chamarizes em medidas de itens, nota global e escolha forçada.

<!-- TODO: incorporar Roy & Robertson (2001), Robertson & Roy (2004) e Jensen & Cardeña (2009) após leitura integral. -->

## 2.2 Críticas metodológicas e resultados nulos

O'Keeffe e Wiseman [-@okeeffe2005] revisaram o debate metodológico sobre testes de médiuns, propuseram um método próprio e não encontraram suporte para capacidade mediúnica genuína em médiuns profissionais. O estudo recebeu críticas quanto à ausência de triagem dos médiuns, à carga de sessões e ao formato dos itens, que agregavam várias afirmações. Em Kelly e Arcangel [-@kelly2011], o cegamento foi considerado incompleto porque os médiuns receberam fotografias dos falecidos. Esses casos mostram que **detalhes operacionais aparentemente pequenos decidem a validade de um estudo**, e é justamente esse tipo de detalhe que um software pode tornar explícito e verificável.

## 2.3 Revisões, meta-análises e o contexto brasileiro

Sarraf, Woodley of Menie e Tressoldi [-@sarraf2021] conduziram uma meta-análise dos estudos controlados de AIR com médiuns. <!-- TODO: inserir tamanho de efeito agregado e heterogeneidade após leitura do texto integral. --> Silva e Moreira-Almeida [-@silva2022] analisaram sete estudos recentes e concluíram que resultados significativos parecem mais prováveis quando o protocolo:
- seleciona médiuns com evidência prévia;
- recruta consulentes motivados;
- combina fala livre com perguntas objetivas;
- pontua tanto leituras globais quanto itens;
- evita volumes excessivos de leituras e itens.

O trabalho busca equilibrar validade ecológica e controle.

No Brasil, Rocha et al. [-@rocha2014] examinaram cartas psicografadas atribuídas a um falecido e recebidas por Chico Xavier. Eles decompuseram o conteúdo em 99 itens objetivamente verificáveis e relataram ajuste elevado. Por ser um estudo de caso retrospectivo, sem controle prospectivo do vazamento, o trabalho ilustra tanto o valor da verificação item a item quanto o que um protocolo prospectivo e cego acrescentaria.

## 2.4 Pré-registro e ciência aberta

Nosek et al. [-@nosek2018] defendem a distinção explícita entre pesquisa confirmatória, com hipóteses e análises fixadas antes dos dados, e pesquisa exploratória, com o pré-registro como mecanismo que torna essa distinção verificável. Numa área em que a credibilidade é o recurso mais escasso, o pré-registro deixa de ser boa prática e passa a ser requisito.

## 2.5 Lacuna

<!-- TODO: levantamento sistemático de ferramentas de software existentes para estudos de mediunidade/psi. Não afirmar ausência de ferramentas sem essa busca. -->

Até onde este levantamento preliminar alcança, os protocolos descritos na literatura são executados com procedimentos majoritariamente manuais. Não identificamos ainda uma plataforma aberta que trate o cegamento, o sorteio e o pré-registro como propriedades verificáveis por terceiros a partir de artefatos exportados.

# 3. Requisitos: um modelo de ameaças de vazamento

Tratamos cada explicação convencional para um acerto como uma **ameaça** a ser mitigada, no vocabulário de engenharia de segurança. A Tabela 1 relaciona ameaças, fontes na literatura e o requisito correspondente.

**Tabela 1. Ameaças à validade e requisitos derivados.**

| Ameaça | Exemplo | Requisito no Crivo |
|---|---|---|
| Pista sensorial ou verbal ao médium | Reações do consulente presente | Consulente ausente; experimentador *proxy* cego [@beischel2007] |
| Informação além do mínimo | Fotografia do falecido [@kelly2011] | Médium recebe só o primeiro nome; qualquer outro dado é vazamento |
| Leitura fria e afirmações genéricas | "Era uma pessoa querida" | Chamariz pareado; só conta o ajuste diferencial alvo × chamariz |
| Itens ambíguos | Item com várias afirmações [@okeeffe2005] | Redação com uma afirmação verificável por item |
| Pista na redação | Primeiro nome ou data na transcrição | Redator cego quanto ao consulente; remoção obrigatória de pistas |
| Viés do avaliador | Consulente sabe qual leitura é a sua | Duas leituras sem rótulo, em ordem sorteada |
| Inferência por metadados | ID sequencial, nome de arquivo, data | IDs aleatórios; remoção de metadados de mídia |
| Sorteio manipulável | Refazer o sorteio até "dar certo" | Gerador criptográfico com compromisso e revelação |
| Flexibilidade analítica | Escolher a análise depois de ver os dados [@nosek2018] | Pré-registro congelado por hash e carimbo de tempo |
| Reescrita da história | Apagar uma sessão desfavorável | Trilha somente de acréscimo, encadeada por hash |
| Viés de publicação | Não divulgar resultado nulo | Relatório e pacote idênticos para qualquer resultado |

# 4. O protocolo pareado

## 4.1 Definições

Uma **leitura** é o conjunto de afirmações que o médium produz numa sessão a respeito de um falecido-alvo. O termo segue a literatura (*reading*) e evita deliberadamente expressões como "comunicação" ou "mensagem", que pressupõem a origem da informação. A **leitura redigida** é a leitura convertida numa lista numerada de **itens**, cada um com uma única afirmação verificável, sem pistas espúrias. Um **par** é formado por dois falecidos-alvo de perfil semelhante (mesmo gênero, faixa etária próxima), e cada um serve de **chamariz** para o outro.

## 4.2 Fluxo

1. O pesquisador redige o pré-registro: hipóteses, critérios de pareamento, escalas, análise primária e secundária, regras de exclusão e tamanho de amostra.
2. Os consulentes consentem e cadastram um falecido-alvo. Os dados identificáveis ficam num repositório separado.
3. O sistema forma os pares e sorteia as alocações. O compromisso criptográfico é publicado, e o pré-registro é congelado.
4. Em cada sessão, o experimentador informa ao médium apenas o primeiro nome do alvo; o médium lê os dois alvos do par.
5. A leitura é transcrita e redigida por alguém que desconhece a qual consulente ela pertence.
6. Cada consulente recebe as duas leituras redigidas do seu par, sem rótulo e em ordem sorteada. Ele pontua cada item, atribui nota global a cada leitura e escolhe qual acredita ser a sua.
7. Depois que todas as avaliações são fechadas, ocorre o desmascaramento, uma operação irreversível e auditada, e a semente do sorteio é revelada.
8. A análise pré-registrada é executada; análises adicionais são rotuladas como exploratórias.

## 4.3 Matriz de visibilidade

O cegamento é especificado como uma matriz de papéis × informações (Tabela 2). Cada célula proibida corresponde a um **teste automatizado que tenta violá-la** contra um banco de dados real.

**Tabela 2. Matriz de visibilidade (resumo).**

| Informação | Pesquisador | Experimentador | Médium | Redator | Consulente |
|---|---|---|---|---|---|
| Identidade do consulente | sim | não | não | não | só a própria |
| Primeiro nome do alvo | sim | sim | na sessão | não | só o próprio |
| Ligação leitura → alvo | só após desmascarar | própria sessão | própria sessão | não | só após desmascarar |
| Leitura redigida | só após desmascarar | não | não | sim | o seu par, sem rótulo |
| Semente do sorteio | só após revelação | não | não | não | não |

## 4.4 Análise

**Medida primária.** A análise confirmatória é a diferença pareada alvo − chamariz, com uma observação por par, avaliada por **teste de Wilcoxon exato unilateral**. A diferença é tomada na nota global ou no percentual de itens com encaixe óbvio, conforme declarado no pré-registro. A escolha forçada, que nas primeiras versões do protocolo era a medida primária, passou a secundária: ela descarta o tamanho da diferença, e Beischel e Schwartz já a esperavam como o indicador menos sensível [@beischel2007]. Com o efeito reportado pela meta-análise de Sarraf et al. [@sarraf2021], a troca decide a viabilidade do estudo: cerca de 30 pares bastam para d = 0,5 com poder de 80% (α = 0,05 unilateral), contra cerca de 240 pares para detectar 58% de acerto na escolha forçada. O cálculo de poder é feito pelo próprio sistema (`PowerCheck`) e entra no documento congelado do pré-registro.

**Unidade de análise: o par.** Os dois consulentes de um par avaliam o mesmo material, em espelho, e por isso suas escolhas não são independentes. Em vez de corrigir o agrupamento com um modelo misto ou erro-padrão robusto por *cluster*, o protocolo fixa o par como unidade independente: antes da primeira sessão, o sorteio designa qual consulente de cada par fornece a observação confirmatória. A avaliação do outro consulente é coletada e relatada como análise secundária.

**Análises secundárias.** Proporção de escolhas forçadas corretas comparada a 0,5 (binomial exato) e **teste de permutação de Pratt e Birge** [@pratt1948] sobre a matriz de notas, que não assume independência entre os itens de uma leitura. A análise vedada é a de multiplicar as probabilidades individuais dos itens como se fossem independentes, procedimento que infla a acurácia porque os itens de uma mesma leitura são correlacionados por construção.

**Regra de parada vinculante.** A coleta para no tamanho-alvo pré-registrado, sem análise interina. Se os dados não correspondem ao declarado — coleta interrompida antes, continuada depois, ou pré-registro não congelado —, o sistema rebaixa o resultado a exploratório e imprime o motivo ao lado (`ConfirmatoryAnalysisPolicy`). Análises não pré-registradas aparecem numa seção exploratória própria.

<!-- TODO: declarar o efeito esperado. Os efeitos por seção de leitura em @beischel2015 vão de d = 0,49 a 0,75, e a seção escolhida muda o tamanho do estudo. -->

# 5. Arquitetura

## 5.1 Visão geral

O Crivo é um monólito modular em Java com Spring Boot e Spring Modulith, persistência em PostgreSQL e interface renderizada no servidor (Thymeleaf e HTMX). Os módulos de domínio são: estudo e pré-registro, alocação, sessão, redação, avaliação, desmascaramento, análise, participantes e auditoria. A escolha por um monólito reflete uma restrição de projeto: o sistema deve ser operável por um único mantenedor, e a complexidade operacional é ela própria uma fonte de erro.

## 5.2 Cegamento como invariante de dados

O cegamento não é aplicado na interface, e sim na **camada de dados**. Cada papel dispõe de projeções próprias (consultas e objetos de transferência) que, **por construção**, não contêm campos proibidos para aquele papel. Isso evita a classe de falhas em que um campo existe na resposta e é apenas "escondido" na tela. Os dados identificáveis ficam num esquema separado do banco (`identity`), acessível apenas pelo módulo de participantes; os demais módulos operam sobre códigos pseudônimos. As fronteiras entre módulos são verificadas por testes de arquitetura.

## 5.3 Sorteio com compromisso e revelação

As alocações são geradas a partir de uma semente $s$ obtida de um gerador criptograficamente seguro, por um algoritmo determinístico e versionado $A$. Antes da primeira sessão, publica-se o compromisso

$$c = \mathrm{SHA\text{-}256}(s \,\|\, \mathrm{canon}(A(s)))$$

em que $\mathrm{canon}$ é uma serialização canônica das alocações. Ao final do estudo, revela-se $s$, e qualquer verificador pode recomputar $A(s)$ e conferir $c$. Isso impede que o sorteio seja refeito após inspeção sem que a alteração seja detectável.

## 5.4 Pré-registro com carimbo de tempo

O pré-registro é serializado de forma canônica, resumido por SHA-256 e ancorado por carimbo de tempo externo (RFC 3161 e/ou OpenTimestamps). Ele também é espelhado num registro público (OSF Registries). Depois de congelado, só admite emendas versionadas, datadas e justificadas, e a versão original permanece visível.

## 5.5 Trilha de auditoria encadeada por hash

Todo evento de domínio (criação, sorteio, sessão, redação, avaliação, desmascaramento, emenda, incidente) é gravado numa tabela somente de acréscimo, em que cada registro $e_i$ carrega

$$h_i = \mathrm{SHA\text{-}256}(h_{i-1} \,\|\, \mathrm{canon}(e_i))$$

O usuário da aplicação no banco não tem permissão de atualizar nem remover registros dessa tabela, o que é imposto pelo próprio SGBD. Uma ferramenta de linha de comando (`airlab verify`) recomputa a cadeia, confere o hash do pré-registro, reproduz o sorteio a partir da semente revelada e reexecuta a análise.

<!-- TODO: discutir ancoragem periódica do último h_i em carimbo de tempo externo, para impedir que um administrador com acesso total reescreva a cadeia inteira (ver Seção 6). -->

## 5.6 Pacote de verificação

Ao final de um estudo, o sistema exporta um pacote com:
- o pré-registro e os comprovantes de carimbo de tempo;
- a semente e as alocações;
- as leituras redigidas;
- as avaliações anonimizadas;
- a trilha de auditoria;
- um script de análise de referência em R.

Dados identificáveis nunca entram no pacote, e uma verificação automática bloqueia a exportação se detectar campos do esquema de identidade. O pacote é publicado em repositório com DOI (por exemplo, Zenodo).

## 5.7 Implantação

O sistema é distribuído como um único artefato executável (um JAR do Spring Boot), acionado por linha de comando em Windows e em Linux, sem container nem orquestrador. Em desenvolvimento, a própria aplicação sobe uma instância embarcada do PostgreSQL, de modo que reproduzir o ambiente exige apenas um JDK — uma decisão deliberada, para que um auditor externo consiga rodar a verificação na sua própria máquina, qualquer que seja o sistema operacional. Em produção, roda numa máquina virtual com dados residentes no Brasil, com o PostgreSQL do sistema operacional não exposto à internet, proxy reverso com HTTPS e backups criptografados fora do servidor. A transcrição de áudio, quando usada, roda **localmente** na máquina do pesquisador (whisper.cpp). Nenhum modelo de IA participa da avaliação ou da pontuação de leituras.

# 6. Garantias e limites

Um software não torna um estudo válido por si só. É necessário explicitar o que o Crivo **garante** e o que **não pode garantir**.

**O Crivo garante, de forma verificável por terceiros:**
- que o pré-registro existia, com o conteúdo publicado, antes da data do carimbo de tempo;
- que o sorteio foi o comprometido antes da coleta;
- que nenhum papel recebeu, **pelo sistema**, informação proibida pela matriz de visibilidade;
- que a sequência de eventos registrada não foi alterada depois de gravada, nos limites discutidos abaixo;
- que a análise relatada é reproduzível a partir do pacote exportado.

**O Crivo não garante:**
- **vazamento fora do sistema**, como conversas entre participantes, buscas em redes sociais pelo primeiro nome do alvo ou conluio entre experimentador e médium. O software reduz e registra superfícies de vazamento, mas não as elimina;
- **integridade contra um administrador com acesso total**, que poderia, em tese, reconstruir a cadeia de auditoria inteira. A mitigação é ancorar periodicamente o estado da cadeia em carimbo de tempo externo e publicar os comprovantes (TODO);
- **validade de construto**: se as escalas e os critérios de pareamento medem o que se pretende medir é uma questão metodológica, não de software;
- **generalização**: resultados de um estudo dizem respeito aos médiuns, consulentes e condições daquele estudo.

Essa delimitação é intencional: um sistema que alega mais do que pode garantir compromete a credibilidade que pretende produzir.

# 7. Considerações éticas e proteção de dados

Os estudos envolvem pessoas enlutadas e dados sobre falecidos, crenças religiosas e, eventualmente, saúde. A Lei Geral de Proteção de Dados Pessoais trata convicção religiosa e filiação a organização religiosa como dados pessoais sensíveis. O Crivo adota minimização, pseudonimização, criptografia em repouso dos campos identificáveis, consentimento específico e versionado e direito de desistência a qualquer momento. Estudos com finalidade de publicação acadêmica devem ser submetidos a Comitê de Ética em Pesquisa (sistema CEP/Conep, via Plataforma Brasil) antes da coleta com participantes reais. A devolutiva aos participantes, inclusive diante de resultados nulos, é tratada como parte do protocolo.

<!-- TODO: citar a LGPD (Lei nº 13.709/2018) e as resoluções do CNS aplicáveis, conferindo o texto vigente. -->

# 8. Estado atual e avaliação planejada

O projeto encontra-se na fase de especificação. Os documentos de visão, princípios e especificação estão disponíveis no repositório. A avaliação planejada tem três etapas:

1. **Testes de propriedade:** suíte automatizada que tenta violar cada célula da matriz de visibilidade e adulterar a trilha de auditoria.
2. **Ensaio simulado (dry-run):** execução completa do protocolo com participantes que simulam os papéis, usando falecidos fictícios, com tentativas deliberadas de quebrar o cegamento.
3. **Auditoria externa:** um pesquisador independente recebe apenas o pacote de verificação e tenta reproduzir a análise e confirmar a integridade do estudo.

Só depois dessas etapas está previsto um estudo piloto com participantes reais.

# 9. Discussão e conclusão

<!-- TODO: desenvolver após a implementação do MVP. Pontos a abordar:
- custo de implantação × ganho de credibilidade;
- replicação multicêntrica: vários grupos usando o mesmo software e o mesmo formato de pacote;
- extensão para protocolos de resposta livre e juízes externos;
- diálogo com pesquisadores céticos como "usuários" do pacote de verificação;
- tensão entre validade ecológica e controle discutida em @silva2022. -->

A credibilidade de um estudo de mediunidade depende de detalhes operacionais que raramente são verificáveis por quem lê o artigo. O Crivo propõe transferir esses detalhes do domínio da confiança para o domínio da verificação: cada regra de cegamento vira um teste, cada sorteio vira um compromisso público e cada evento vira um elo numa cadeia de hashes. O objetivo não é favorecer uma conclusão, e sim que **qualquer conclusão, positiva ou nula, possa ser conferida por quem duvida dela**.

# Disponibilidade

Código-fonte e documentação: <https://github.com/rogeriocisi/crivo>.

<!-- TODO: licença do software; DOI do Zenodo quando houver release. -->

# Referências

::: {#refs}
:::

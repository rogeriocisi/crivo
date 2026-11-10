# Plano técnico do MVP — Crivo (AIR Lab)

> Derivado de `CONSTITUTION.md` (prevalece) e de `SPEC.md`. Versão 1.2 · novembro/2026.
> Este documento registra as decisões de implementação do MVP: estrutura de módulos,
> esquemas e migrações, execução por linha de comando e justificativa de cada dependência.

---

## 1. Arquitetura de execução (sem Docker)

| Item | Decisão |
|---|---|
| Artefato | **Um JAR executável** (`target/airlab.jar`), com Tomcat embutido |
| Execução | Linha de comando, em Windows e Linux: `scripts/start.sh`, `scripts/start.ps1`, `scripts/start.cmd` |
| Build | Maven Wrapper (`mvnw`), `distributionType=only-script` — **nenhum JAR versionado** no repositório |
| Pré-requisito | Um JDK 21 (LTS). Nada mais |
| Banco em desenvolvimento e teste | **PostgreSQL embarcado** pela aplicação (`io.zonky.test:embedded-postgres`), binários reais do PostgreSQL, em `~/.airlab/pgdata` (fora do repositório e fora de pasta sincronizada) |
| Banco em produção | PostgreSQL do sistema operacional, escutando só em `127.0.0.1` |
| Processo em produção | Unidade `systemd` (Linux) ou serviço do Windows / Agendador de Tarefas |
| Borda em produção | Caddy instalado como pacote do sistema, HTTPS automático, proxy para `127.0.0.1:8080` |
| CLI de auditoria | Mesmo JAR: `scripts/airlab.sh verify`, `scripts\airlab.ps1 verify` |

**Por que o PostgreSQL continua obrigatório**: o P4 (trilha append-only) e o P6 (separação de
esquemas com permissões por papel) são impostos *no banco*. H2 ou SQLite não oferecem papéis,
esquemas e triggers equivalentes, então trocar de banco enfraqueceria dois princípios para
ganhar conveniência. A solução adotada mantém o PostgreSQL e remove o Docker: em
desenvolvimento ele é embarcado no processo da aplicação; em produção é um serviço do sistema.

**Paridade Windows/Linux**: nenhum caminho absoluto no código; diretórios vêm de configuração
com padrão relativo ao diretório de trabalho; o documento do pré-registro usa sempre `\n`, para
que o hash não dependa do sistema operacional.

## 2. Estrutura de módulos (Spring Modulith)

Pacote base `io.github.rogeriocisi.airlab`. Cada módulo declara em `package-info.java` de
quem pode depender; `ModularityTest` verifica isso no build.

| Módulo | Épico | Estado |
|---|---|---|
| `audit` | E8 | **Implementado**: trilha append-only encadeada por hash, verificação da cadeia |
| `allocation` | E3 | **Implementado**: sorteio reproduzível, compromisso → revelação, custódia da semente |
| `study` | E1 | **Implementado**: estudo, documento canônico do pré-registro, congelamento, emendas |
| `participant` | E2 | **Implementado**: participantes, consentimento, alvos, pareamento, desistência |
| `redaction` | E5 | Parcial: `ClueScrubber` (guarda de pistas). Persistência e telas pendentes |
| `analysis` | E7 | Parcial: Wilcoxon pareado exato (medida primária), binomial exato, permutação de Pratt e Birge, `PowerCheck`, política de rótulo e redução às unidades independentes. Persistência e relatório pendentes |
| `session` | E4 | Pendente (tabelas prontas; os slots contrabalanceados já saem do sorteio) |
| `scoring` | E6 | Parcial: escala de cinco categorias com justificativa obrigatória e a lógica do link de uso único (`AccessLink`). Pacotes e telas pendentes |
| `unblinding` | E7 | Pendente (tabelas prontas) |
| `cli` | E8 | **Implementado**: `airlab verify` |
| `catalog` | — | **Implementado**: catálogo de protocolos (SPEC 1.1), sem estado e sem dependência de outro módulo |
| `trials` | E11, E12 | **Implementado**: sessões de autoatendimento dos dois testes de ESP — semente comprometida, alvo derivado só no travamento, impressão travada antes das candidatas, sessão abandonada registrada |
| `support`, `platform`, `web` | — | Infraestrutura e páginas públicas |

Dependências entre módulos: `audit` não depende de nenhum módulo de domínio; todos os outros
dependem dele. O plano de alocação sai do módulo `allocation` apenas como evento de domínio
(`AllocationDrawn`), nunca por consulta, porque o pesquisador não pode ver a ligação
leitura → alvo antes do desmascaramento (SPEC, seção 4).

## 3. Esquemas e migrações

Flyway, em `src/main/resources/db/migration`. Histórico no esquema `research`.

| Migração | Conteúdo |
|---|---|
| `V1` | Esquemas `research`, `identity`, `audit` |
| `V2` | `audit.event` + triggers que recusam UPDATE, DELETE e TRUNCATE (P4) |
| `V3` | `research.study`, `research.preregistration_version` + trigger de imutabilidade do congelado (P2) |
| `V4` | `identity.participant`, `identity.target_detail`, `identity.consent`, `research.participant_role`, `research.target`, `research.pair` (P6) |
| `V5` | `research.allocation_set` + trigger de imutabilidade do sorteio; semente sob custódia cifrada (P5) |
| `V6` | Sessões, leituras brutas, ocorrências, incidentes (E4) |
| `V7` | Leituras redigidas e itens (E5) |
| `V8` | Pacotes de avaliação, pontuações, escolha forçada (E6) |
| `V9` | Desmascaramento (append-only) e execuções de análise (E7) |
| `V10` | Papel `airlab_app` com privilégio mínimo: na trilha, só `SELECT` e `INSERT` |
| `V11` | Escala de itens de cinco categorias, com justificativa obrigatória imposta por `CHECK` (E6) |
| `V12` | `research.protocol_deviation`, append-only por *trigger* (E1/E7) |
| `V13` | Comentários em português passam a dizer **consulente** no lugar de *sitter* (SPEC 0.5) |
| `V14` | Índice cego do contato em `identity.participant`, para detectar papel duplo sem guardar contato em claro (E10) |
| `V15` | `research.test_session` e `research.test_trial` dos testes de autoatendimento, com `CHECK` que impede alvo antes do travamento e *trigger* que torna a tentativa travada imutável (E11/E12) |
| `V16` | Entropia do participante: `participant_nonce` e `server_seed_hex`, *trigger* que recusa tentativa de sessão sem a metade do participante, e preenchimento das sessões anteriores (`server_seed_hex = seed_hex`, porque antes a semente era toda do servidor) |

**Migração aplicada não se edita**: corrigir o vocabulário dos comentários mudaria o checksum
do Flyway e quebraria a validação em qualquer banco já migrado — inclusive o do próprio autor.
Por isso a troca de `sitter` para `consulente` nos comentários veio como `V13`, e não como edição
das migrações anteriores.

**Garantias no banco, não só no código**: os triggers alcançam qualquer papel, inclusive
superusuário, de modo que a imposição vale também no perfil local, onde a aplicação conecta
como proprietária do banco.

## 4. Os dois testes de autoatendimento (E11 e E12)

A plataforma passou a cobrir mais de um protocolo. O catálogo está na SPEC, seção 1.1; aqui
ficam as decisões de implementação.

| Decisão | Por quê |
|---|---|
| Um módulo só (`trials`) para os dois protocolos | Eles compartilham tudo que importa: sessão com N fixo, semente comprometida, alvo derivado no travamento, revelação no fim. O que difere é um passo a mais na visão remota |
| Alvo derivado, não sorteado e guardado | `alvo(n) = SeedStream(semente, "trial:" + n) mod alternativas`. Nada é gravado antes da resposta: a coluna `target_position` é nula até o travamento, imposto por `CHECK` |
| **Semente em duas metades** | `semente = SHA-256(metade do servidor \|\| metade do participante)`. O servidor publica o hash da metade dele antes de a outra existir; o navegador sorteia a dele com `crypto.getRandomValues`. Nenhum dos lados escolhe o resultado, e a prova deixa de depender de ordem atestada pela plataforma |
| Sem a metade do participante, nada existe | Nem semente, nem alvo, nem linha de tentativa. Imposto por *trigger* (V16), não só no serviço: é essa ordem que sustenta a afirmação do protocolo |
| O compromisso é guardado pelo próprio navegador | `sessionStorage`, no momento em que a tela da entropia chega. No fim, o verificador compara — a conferência que antes exigia a pessoa copiar o hash à mão. Quando não há armazenamento, a página diz que essa parte não foi conferida |
| Semente em custódia cifrada, revelada no fechamento | Mesmo esquema do sorteio do P1. Um dump do banco durante a sessão não entrega o alvo |
| Compromisso liga a semente aos parâmetros | `SHA-256(semente \| protocolo \| tentativas \| alternativas \| versão do banco)`. Sem isso, um compromisso poderia ser "honrado" por uma sessão de outro tamanho |
| Impressão travada antes das candidatas (P3) | A trilha guarda só o hash da impressão. É o que sustenta a afirmação de que o texto não foi reescrito depois de ver as figuras — objeção padrão ao autojulgamento |
| Banco de figuras versionado no repositório, não no banco de dados | A versão usada fica na sessão. Assim a aplicação e o pacote de verificação não podem discordar sobre o que era a figura 7 |
| Sessão anônima, sem conta nem e-mail | Não há nada aqui que justifique pedir dado pessoal, e é isso que permite publicar o conjunto |
| `SessionProgress` sem nenhum campo de desfecho | Trava 1 do E10 aplicada ao autoatendimento. Há teste estrutural: um campo com nome de acerto, escore ou alvo derruba o build |
| Alvo e acerto fora da trilha por tentativa | A trilha é pública. Publicar o acerto de cada tentativa entregaria o placar que a sessão esconde; eles entram no evento de fechamento, com a semente |
| Sessão abandonada é registrada e fica | Desistir de uma sessão ruim não melhora o número agregado |

**O que estes dois testes não fazem**: não dizem nada sobre mediunidade, e o pool público é
exploratório — autoatendimento anônimo não controla quem testa, quantas vezes nem em que
condições. Para resultado confirmatório, vale a política de rótulo que já existe: pré-registro
congelado, participantes identificados e N declarado antes da coleta.

## 5. Decisões metodológicas impostas pelo código

Críticas recorrentes a esta literatura que não se resolvem com disciplina do pesquisador; o
sistema as trata como regra:

| Crítica | Como o sistema responde | Onde |
|---|---|---|
| Resultados positivos vêm de "médiuns selecionados", sem critérios publicados | `mediumInclusionCriteria` é campo obrigatório do pré-registro, entra no documento com hash antes da coleta | `study.PreregistrationPlan` |
| Parada opcional: encerrar a coleta quando os números ficam bons | A análise só recebe o rótulo **confirmatória** se o pré-registro estava congelado e o número de unidades coletadas for exatamente o alvo declarado. Fora disso, sai rotulada como exploratória, com o motivo impresso ao lado | `analysis.ConfirmatoryAnalysisPolicy` |
| Dependência entre os dois consulentes de um par, tratados como observações independentes | A unidade da análise confirmatória é o par: entra só a escolha do consulente **sorteado** para aquele par, antes da primeira sessão. A outra escolha é relatada como secundária | `allocation.AllocationPlan.ConfirmatoryUnit`, `analysis.IndependentUnits` |
| Estudo subdimensionado cujo resultado nulo é apresentado como ausência de efeito | O mínimo de pares para o efeito declarado é calculado e **escrito no documento congelado**, com a frase que diz se o desenho atende ou não | `analysis.PowerCheck` |
| Comprimento da leitura como pista e como confundidor | Tolerância pré-registrada de diferença de itens; a leitura mais longa é cortada por subamostra sorteada da semente | `PreregistrationPlan.maxItemCountDifference`, `AllocationEngine.balancedSubsample` |
| Pistas temporais que permitem ao consulente reconhecer a própria leitura | Itens apresentados em ordem sorteada; slots de sessão contrabalanceados por quadrado latino, sorteados junto com o resto | `AllocationEngine.itemPresentationOrder`, `AllocationPlan.SessionSlot` |
| Ordem de apresentação desequilibrada dentro do par | Um consulente vê a própria leitura primeiro e o outro vê o chamariz; qual dos dois é sorteado | `AllocationEngine` (domínio `presentation:`) |
| Item que se encaixa em outra pessoa próxima, forçado a virar acerto ou erro | Categoria própria na escala, com explicação escrita obrigatória, imposta também por `CHECK` no banco | `scoring.ItemScoreValue`, `scoring.ItemScoreEntry`, migração `V11` |
| Execução que se afasta do plano congelado e não aparece no relatório | Desvio de protocolo é entidade própria, append-only, distinta de emenda | `study.ProtocolDeviation`, migração `V12` |
| Multiplicar probabilidades de itens como se fossem independentes | Análise vedada. A alternativa do campo é o teste de permutação de Pratt e Birge | `analysis.PermutationTest` |
| Painel de progresso que vira análise interina contínua | A projeção de progresso não tem campo de desfecho, e um teste estrutural reprova o build se alguém acrescentar um | `analysis.CollectionProgress` |
| Conluio na inscrição aberta: a mesma pessoa como médium e como consulente | Um papel por pessoa em cada estudo, reconhecida por índice cego do contato; a recusa vai para a trilha | `support.BlindIndex`, migração `V14` |
| Link de avaliação reaproveitado, vazado ou reaberto depois do envio | Token de uso único guardado só como hash, com prazo, comparação em tempo constante e recusa definitiva após o envio | `scoring.AccessLink` |

O sistema não pretende impedir que uma coleta seja interrompida — isso está fora do alcance de
software. O que ele faz é recusar o rótulo de confirmatória nesse caso, o que é a parte que
importa para quem lê o relatório.

### Medida primária e poder

Com o par como unidade, as ~70 observações do cálculo original passariam a significar 70
pares — 140 consulentes e 140 sessões. Pior: a meta-análise de Sarraf, Woodley of Menie e
Tressoldi (2021) estima o efeito do campo em **6 a 14 pontos acima do acaso**, e a 58% de
acerto aquelas 70 unidades dariam cerca de **32% de poder**.

Mínimos para α = 0,05 unilateral e poder de 0,80, conforme `PowerCheck` (com valores de
referência nos testes):

| Medida primária | Efeito declarado | Pares necessários |
|---|---|---|
| Escolha forçada | 56% de acerto | 428 |
| Escolha forçada | 58% | 240 |
| Escolha forçada | 60% | 153 |
| Escolha forçada | 65% | 67 |
| Diferença pareada | d = 0,49 | 30 |
| Diferença pareada | d = 0,50 | 29 |
| Diferença pareada | d = 0,57 | 22 |
| Diferença pareada | d = 0,75 | 13 |

Daí a decisão da v0.4 da SPEC: **a medida primária passa a ser a diferença pareada
alvo − chamariz**, testada com Wilcoxon exato, o que mantém o par como unidade independente e
recupera o poder. A escolha forçada continua coletada e relatada como secundária. Beischel e
Schwartz já esperavam que ela fosse o indicador menos sensível, e os números deles
confirmaram (notas globais p = 0,007; escolha forçada p = 0,01).

O que resta decidir é qual efeito declarar: os efeitos por seção de leitura em Beischel et al.
(2015) vão de d = 0,49 a 0,75.

## 6. Decisões criptográficas

| Decisão | Por quê |
|---|---|
| Hash da trilha sobre JSON canônico, **sem o número de sequência** | O auditor precisa recomputar os hashes a partir do pacote exportado, sem saber como as linhas foram numeradas |
| Semente de 32 bytes do `SecureRandom` | P5, imprevisibilidade |
| Expansão da semente por **SHA-256 em modo contador** (`SeedStream`), não por `SecureRandom` | Nenhum algoritmo de `SecureRandom` garante o mesmo resultado em outra JVM ou outro sistema; o sorteio tem de ser reproduzível por terceiros |
| Um domínio de expansão por decisão (`medium:`, `reading-order:`, `experimenter:`, `presentation:`) | Acrescentar uma decisão no futuro não desloca as decisões anteriores |
| Semente cifrada em custódia (AES-256-GCM) até o desmascaramento | Um dump do banco durante o estudo não revela a semente, e a semente sobrevive a um reinício |
| Campos identificáveis cifrados com AES-256-GCM, envelope `v1:` | LGPD (P6) e espaço para rotação de chave |

## 7. Dependências novas e sua justificativa (P8)

| Dependência | Por quê | Custo de manutenção |
|---|---|---|
| `spring-boot-starter-web`, `-thymeleaf`, `-data-jpa`, `-security`, `-validation`, `-actuator` | Stack de referência da Constitution | Baixo, versões alinhadas pelo parent |
| `spring-modulith-starter-core` / `-test` | Fronteiras de módulo verificadas em teste (regra arquitetural 1) | Baixo |
| `flyway-core`, `flyway-database-postgresql` | Migrações auditáveis | Baixo |
| `postgresql` (driver) | Banco de referência | Baixo |
| `io.zonky.test:embedded-postgres` + binários `linux-amd64` e `windows-amd64` (PostgreSQL 16.9) | **Substitui o Testcontainers e o Docker Compose**: PostgreSQL real em desenvolvimento e teste sem container, nos dois sistemas operacionais | Médio: é preciso acompanhar a versão dos binários do PostgreSQL. Aceito porque remove a dependência de Docker, exigida pela regra arquitetural 8. **Os binários 17.x falham no `initdb` em Windows** (`coluna "attnum" não existe`); a versão está fixada em 16.9.0 e só deve subir depois de testada nos dois sistemas |
| Nenhuma biblioteca de estatística | A análise primária é um teste binomial exato, escrito em aritmética racional (~40 linhas) e validado contra valores de referência | Nenhum |
| Nenhuma biblioteca de JSON para hashing | A forma canônica é escrita no projeto (`CanonicalJson`), porque o hash não pode mudar quando uma biblioteca muda de versão | Nenhum |

Jackson entra apenas como já parte do starter web, e só é usado para **ler** a entrada do
sorteio na CLI, nunca para calcular hash.

## 8. Testes

| Teste | Princípio | Tipo |
|---|---|---|
| `AuditTrailIntegrationTest` | P4 | PostgreSQL real: cadeia verificável, UPDATE/DELETE recusados, linha forjada detectada |
| `PreregistrationIntegrationTest` | P2, P5 | PostgreSQL real: congelamento, imutabilidade, emenda, sorteio único, revelação da semente |
| `AllocationEngineTest` | P5 | Unitário: reprodutibilidade, independência da ordem de entrada, compromisso ligado às alocações |
| `BinomialTestTest` | P9 | Unitário: valores de referência exatos (conferem com `binom.test` do R) |
| `ConfirmatoryAnalysisPolicyTest` | P2 | Unitário: parada antes e depois do alvo, pré-registro não congelado e unidade dependente viram exploratória |
| `IndependentUnitsTest` | P9 | Unitário: uma escolha por par, a do consulente sorteado; dado faltante interrompe em vez de encolher a amostra |
| `ClueScrubberTest` | P1 | Unitário: primeiro nome, pistas de sessão, datas, item com mais de uma afirmação |
| `BlindingMatrixTest` | P1 | Estrutural: campo proibido ausente da projeção, não filtrado na view |
| `PairedPrimaryMeasureTest` | P9 | Wilcoxon exato contra valores de referência; par duplicado recusado |
| `PowerCheckTest` | P9 | A tabela de poder da seção 4, como teste |
| `PermutationTestTest` | P9 | Permutação exata; reproduz o resultado nulo publicado por O'Keeffe e Wiseman |
| `ItemScoreEntryTest` | P1, E6 | Justificativa obrigatória nos valores interpretativos; só o encaixe óbvio conta |
| `ModularityTest` | Regra 1 e 2 | Fronteiras de módulo |
| `TargetDerivationTest` | P5 | Unitário: alvo reproduzível pela semente, dentro do intervalo, candidatas distintas e banco pequeno recusado |
| `ProtocolCatalogTest` | SPEC 1.1 | Unitário: todo protocolo declara o que não sustenta, e protocolo sem limites não compila em tempo de execução |
| `TrialSessionIntegrationTest` | E10 trava 1, E11, E12 | PostgreSQL real: semente só no fim, alvo ausente do banco antes do travamento, impressão não reescrita, tentativa travada imutável, fechamento idempotente, **nada acontece antes da entropia do participante — recusado no serviço e pelo *trigger*** |
| `TargetDerivationTest` (semente conjunta) | E11 | Unitário: vetor de teste das duas metades, e a ordem das metades faz parte da regra |
| `TrialRunControllerTest` | E11, E12 | Páginas: passo da entropia antes de qualquer tentativa, reenvio retoma o teste, coluna de figuras só onde há figuras |

Os testes de integração usam o mesmo PostgreSQL embarcado, então `./mvnw test` funciona em
Windows e Linux sem Docker e sem banco instalado.

## 9. Armadilhas conhecidas

1. **Binários do PostgreSQL embarcado**: fixados em 16.9.0. Os 17.x quebram o `initdb` no Windows.
2. **Diretório do cluster**: nunca aponte `airlab.local-database.data-directory` para pasta
   sincronizada (Google Drive, OneDrive) nem para disco de rede — o `initdb` falha ou o cluster
   corrompe. O padrão é `~/.airlab/pgdata`, e os testes usam o diretório temporário do sistema.
3. **Precisão de instante na trilha**: o `occurredAt` é truncado a microssegundos antes de
   entrar no hash, porque é essa a precisão que o PostgreSQL grava. Sem isso, a releitura
   recomputa outro hash e a cadeia parece quebrada.
4. **Tamanho do JAR**: ~150 MB, porque carrega os binários do PostgreSQL das duas plataformas.
   Aceitável para implantação por `scp`; se incomodar, separe um perfil Maven que exclua os
   binários no artefato de produção.

5. **`target/` dentro de pasta sincronizada**: o JAR tem ~150 MB e o `target/` fica no Google
   Drive junto com o repositório. O Drive segura o arquivo enquanto sincroniza, e o
   `spring-boot:repackage` falha com `Unable to rename ... airlab.jar`. E não é só o JAR: já
   aconteceu de `mvn clean` não conseguir apagar `target/classes` e de uma compilação enxergar
   classes principais ausentes que estavam lá. Sintoma a reconhecer: erro de I/O ou símbolo não
   encontrado que **some ao construir noutro lugar**.

   Contorno imediato: apagar `target/airlab.jar` e repetir. Contorno confiável, e o único que
   resolveu os três casos:

   ```
   ./mvnw -Dmaven.build.directory=C:/temp/airlab-target test
   ```

   Correção de verdade: manter o repositório num disco local. O `target/` é gerado, não deveria
   estar sincronizado, e um JAR de 150 MB subindo para a nuvem a cada build é desperdício além
   de risco.
6. **Parar a aplicação pelo gerenciador de tarefas deixa o cluster travado**: o
   `epg-lock` em `~/.airlab/pgdata` fica preso pela JVM, e a próxima execução falha com
   `could not lock`. Encerre com `Ctrl+C` no terminal do `start.sh`, que roda o desligamento
   limpo. Se já aconteceu, encerre o processo `java -jar target/airlab.jar` e o `postgres.exe`
   correspondente — e **só esses**: é fácil derrubar junto o servidor de linguagem Java do IDE,
   que também é `java.exe`.

7. **Um cluster por JVM, não por contexto Spring**: a suíte carrega vários contextos no mesmo
   processo, e cada um tentando subir o próprio PostgreSQL embarcado na mesma porta falha com
   `could not lock`. Por isso `LocalDatabaseConfiguration` compartilha a instância no processo e
   a encerra por *shutdown hook*, não por fechamento de contexto. Ao criar um teste que force
   contexto novo (por exemplo com `@MockitoBean`), nada precisa ser feito — mas não volte a
   registrar `destroyMethod` no bean.


## 10. Próximos cortes

1. **E4 (sessões)**: consumir `AllocationDrawn`, criar sessões, tela do experimentador com
   apenas o primeiro nome, upload de mídia com identificador aleatório. É aqui que a unidade
   confirmatória sorteada precisa ser persistida junto com as sessões, para alimentar o E7.
2. **E5 (redação)**: persistência das leituras redigidas e itens, usando o `ClueScrubber` como
   validação de submissão.
3. **E6 (avaliação cega)**: links de uso único e expiráveis, formulário do consulente, bloqueio no
   envio, mais as linhas da matriz de cegamento que dependem de estado persistido.
4. **E7 (desmascaramento e análise)**: operação irreversível depois do fechamento de todas as
   avaliações, execução da análise primária, relatório neutro.
5. **E8/E9**: carimbo de tempo RFC 3161, pacote de verificação e script de análise em R.

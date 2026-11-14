# CLAUDE.md — Crivo (AIR Lab)

Instruções para o Claude Code neste repositório.

## Leia antes de qualquer tarefa

1. `docs/CONSTITUTION.md`: princípios inegociáveis P1–P10, arquitetura e implantação. **Prevalece sobre todo o resto.**
2. `docs/SPEC.md`: especificação macro, com o **catálogo de protocolos (seção 1.1)**, a matriz de cegamento (seção 4) e os épicos E1–E12. O **E10 traz as travas obrigatórias da automação**; nenhuma tela de autoatendimento entra sem elas. E11 e E12 são os dois testes integralmente automatizados.
3. `docs/PLAN.md`: plano técnico do MVP — módulos, migrações, decisões criptográficas, dependências e **estado de implementação por épico**.
4. `docs/VISION.md`: contexto, objetivos e não objetivos.
5. `docs/REFERENCES.md`: fundamentação científica. Ao mudar uma regra do protocolo, verifique se ela contraria alguma decisão mapeada ali.

## Regras de trabalho

- **Spec-driven**: Constitution → Spec → Plano técnico → Tarefas → Implementação. Não implemente funcionalidade sem spec correspondente; se faltar, proponha a spec primeiro.
- **Antes de propor um plano ou código, verifique a conformidade com P1–P10.** Se algo violar um princípio, pare e aponte.
- **Cegamento (P1)**: toda regra da matriz da SPEC, seção 4, tem teste que tenta violá-la. Nunca desative, pule ou marque como `@Disabled` testes de cegamento ou de integridade da trilha de auditoria.
- **Auditoria (P4)**: toda operação de domínio emite um evento de auditoria. Operação sem evento é bug.
- **Privacidade (P6)**: nada de dados pessoais em logs, mensagens de exceção, nomes de arquivo ou fixtures com dados reais. Apenas o módulo `participant` acessa o esquema `identity`.
- **IA (P10)**: nenhum código usa IA para avaliar leituras ou pontuar.
- **Nenhum resultado parcial antes do desmascaramento (E10, trava 1)**: nenhuma tela, API ou projeção pode expor função dos escores enquanto o estudo corre. Progresso é contagem de unidades, nunca desempenho; há teste estrutural em `BlindingMatrixTest`. Vale também para as sessões de autoatendimento: `trials.SessionProgress` não tem campo de desfecho, e o alvo de uma tentativa não existe no banco antes do travamento.
- **Escopo da plataforma**: o Crivo cobre **cognição anômala**, com a mediunidade (AIR) como um ramo e os testes de ESP como outro. O ramo AIR tem **um protocolo ativo: o P4, leitura estruturada automatizada** (E13). O **P1, leitura pareada conduzida por equipe, está descontinuado** (SPEC 0.14): não implemente nada dele — nem E4 (sessões), nem E5 (redação), nem E6 (avaliação cega), nem E7 (desmascaramento do P1). O código que já existe e é compartilhado com o P4 (`study`, `allocation`, `participant`, `analysis`, `audit`) fica; os módulos `session`, `redaction`, `scoring` e `unblinding` ficam como estão, sem evolução. No site, o P1 fica **apenas como referência**: a página `/testes/leitura-pareada` descreve o desenho, sem inscrição; o catálogo o mantém com status `DISCONTINUED` ("Só referência, não implementado"), fora da lista de protocolos para participar. Os testes de ESP aparecem como **experimentos extras**. Não chame um teste de precognição de AIR: na literatura, AIR é informação sobre pessoa falecida. O codinome técnico **AIR Lab** permanece em pacotes, CLI e artefatos Maven, por herança do primeiro protocolo.
- **Todo protocolo novo entra pelo catálogo** (`catalog.ProtocolCatalog`, SPEC 1.1) e **declara o que o resultado dele não sustenta**. O construtor de `Protocol` recusa lista de limites vazia, e há teste que exige isso de cada protocolo.
- **Um papel por pessoa em cada estudo (E10, trava 2)**: recusado na camada de domínio e registrado na trilha.
- **Contas do P4 (E13)**: perfil fixo (consulente ou médium), imposto por *trigger*; nenhum caminho do site cria administrador, que vem da configuração do servidor. O principal da sessão é o código pseudônimo, nunca o e-mail. Avisos ao administrador carregam só tipo, código e hora. No P4, nenhuma nota existe antes do desmascaramento: a conferência só roda depois dele. **Nenhum código de médium vai à trilha** em envio, liberação de nome ou leitura antes do desmascaramento, e o primeiro nome só sai de `participant` para o médium com liberação registrada. Os testes do P4 dividem um banco persistente: use um estrato próprio por teste e o `structured.P4Fixture`, e nomes de conta únicos (`TestNames.unique`), porque nomes iguais viram sinalização de contas ligadas. O que precisa de um estudo que complete (desmascaramento) roda em banco próprio, recriado a cada execução — ver `web.P4UnblindingTest`.
- **Análise vedada**: nunca multiplicar as probabilidades individuais dos itens como se fossem independentes (Hyslop, 1919). Para dados de acurácia de leitura, a análise do campo é o teste de permutação de Pratt e Birge (`analysis.PermutationTest`).
- **Reprodutibilidade do sorteio (P5)**: tudo que o sorteio decide tem de ser independente da ordem em que as entradas chegam. Ordene em forma canônica antes de usar a semente; há teste que tenta violar isso.
- **Simplicidade (P8)**: monólito modular; justifique por escrito qualquer dependência nova.
- **Versões**: confira a LTS atual do Java e a versão estável atual do Spring Boot e do Spring Modulith antes de fixá-las.
- **Sem Docker (regra arquitetural 8)**: nenhuma etapa obrigatória pode exigir Docker, container ou orquestrador. Em desenvolvimento e teste o PostgreSQL é embarcado pela aplicação (`io.zonky.test:embedded-postgres`); em produção é o do sistema operacional. Não reintroduza Testcontainers nem `docker-compose.yml`.
- **Paridade Windows/Linux**: todo script e comando precisa funcionar nos dois. Nada de caminho absoluto no estilo `/var/...`; diretórios vêm de configuração. O que é acionável por linha de comando tem versão `.sh` e `.ps1` em `scripts/`.
- **Script PowerShell**: nunca redirecione a saída de erro de executável nativo (`algum.exe ... 2>&1`). O PowerShell embrulha cada linha num `ErrorRecord` e, com `$ErrorActionPreference = 'Stop'`, aborta o script por causa de uma saída perfeitamente normal. Foi assim que `java -version` derrubou o `start.cmd`; a forma correta é `java --version`, que escreve na saída padrão. No Windows, o caminho recomendado é o `scripts\start.cmd`: ele chama o `.ps1` com `-ExecutionPolicy Bypass`, porque a política padrão do Windows recusa script não assinado. Nunca altere a política de execução da máquina do usuário.
- **Execução**: a aplicação sobe por um script único (`scripts/start.sh`, `scripts/start.ps1`), que também sobe o banco. Ao acrescentar um componente obrigatório, inclua-o nesse script ou justifique por escrito.

## Convenções

- Pacote base: `io.github.rogeriocisi.airlab`; módulos `study`, `allocation`, `session`, `redaction`, `scoring`, `unblinding`, `analysis`, `participant`, `audit`, `catalog`, `trials`, `notification`, `structured`.
- Código, identificadores e commits em **inglês**; documentação e interface em **português**.
- Commits pequenos, no padrão Conventional Commits.
- Vocabulário neutro (P3): *reading*, *item*, *target*, *decoy*, *score*. Nunca linguagem que pressuponha a origem da informação.
- **Consulente, não *sitter***: em português — documentação, interface, comentários em português — quem avalia as leituras é o **consulente**. A forma inglesa *sitter* fica registrada no glossário da SPEC, por ser o termo da literatura, e permanece nos identificadores do código (`sitterCode`, `sitter_code`, `ParticipantRole.SITTER`), pela regra de código em inglês.

## Estado e ordem do MVP

Já implementado (ver `docs/PLAN.md`, seção 2, para a tabela completa):

1. Plano técnico, migrações `V1`–`V21` dos esquemas `research`/`identity`/`audit` e papel `airlab_app`
2. `audit`: trilha append-only encadeada por hash + CLI `airlab verify`
3. `allocation`: sorteio reproduzível (`SecureRandom` para a semente, SHA-256 em modo contador para a expansão), compromisso → revelação, custódia cifrada da semente
4. `study` (E1) e `participant` (E2), com pareamento
5. Partes puras de E5 (`ClueScrubber`) e E7 (Wilcoxon pareado exato como medida primária, binomial exato, permutação de Pratt e Birge, `PowerCheck`)
6. Escala de itens de cinco categorias com justificativa obrigatória (E6) e desvios de protocolo append-only (E1/E7)
7. `catalog` (SPEC 1.1) e `trials` (E11, E12): seção de testes no site e os dois protocolos de ESP integralmente automatizados, com semente comprometida, alvo derivado no travamento e impressão travada antes das candidatas
8. E13, fase A: catálogo em duas seções (protocolos AIR e experimentos extras), páginas do P4, contas com perfil fixo, confirmação por e-mail, painéis por perfil e avisos ao administrador
9. E13, fase B: questionário versionado (`catalog.Questionnaires`, `v1-rascunho`) e pedido de consulta travado na criação, com compromisso salgado (`participant.ConsultationCommitment`); rascunho só na sessão
10. E13, fase C (módulo `structured`): estudo P4 congelado com o questionário no anexo e compromisso sobre a semente; ciclo agendado que pareia, sorteia o médium, libera os nomes (`identity.first_name_release`), vence prazos e reenvia; leitura do médium travada com compromisso salgado (`structured.ReadingCommitment`)
11. E13, fase D: desmascaramento automático no N (`structured.internal.StructuredUnblinding`), compromissos conferidos, conferência (`structured.StructuredScoring`), Wilcoxon, sinal e permutação dentro dos estratos (`analysis.StratifiedPermutationTest`), relatório com hash e devolutiva (`structured.FeedbackService`); notas e relatório recusados pelo banco antes do desmascaramento
12. E13, fase E: sinalização de contas ligadas por índice cego, sem bloqueio e com revisão humana (`AccountService.flags`, `keptApart`); teto de pares por médium no pré-registro; página pública `/estudos`

A fazer, só no P4: ancoragem externa da trilha e do pré-registro por carimbo de tempo RFC 3161 (E8); pacote de verificação do P4 (E9), que depende da decisão de LGPD sobre publicar respostas; e o que a SPEC, seção 9, ainda deixa em aberto para o P4 (questões 9, 11 e 12).

A suíte da matriz de cegamento cresce junto: cada linha nova da matriz da SPEC, seção 4, entra como teste antes da tela correspondente.

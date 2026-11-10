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
- **Escopo da plataforma**: o Crivo cobre **cognição anômala**, com a mediunidade (AIR) como um ramo e os testes de ESP como outro. Não chame um teste de precognição de AIR: na literatura, AIR é informação sobre pessoa falecida. O codinome técnico **AIR Lab** permanece em pacotes, CLI e artefatos Maven, por herança do primeiro protocolo.
- **Todo protocolo novo entra pelo catálogo** (`catalog.ProtocolCatalog`, SPEC 1.1) e **declara o que o resultado dele não sustenta**. O construtor de `Protocol` recusa lista de limites vazia, e há teste que exige isso de cada protocolo.
- **Um papel por pessoa em cada estudo (E10, trava 2)**: recusado na camada de domínio e registrado na trilha.
- **Análise vedada**: nunca multiplicar as probabilidades individuais dos itens como se fossem independentes (Hyslop, 1919). Para dados de acurácia de leitura, a análise do campo é o teste de permutação de Pratt e Birge (`analysis.PermutationTest`).
- **Reprodutibilidade do sorteio (P5)**: tudo que o sorteio decide tem de ser independente da ordem em que as entradas chegam. Ordene em forma canônica antes de usar a semente; há teste que tenta violar isso.
- **Simplicidade (P8)**: monólito modular; justifique por escrito qualquer dependência nova.
- **Versões**: confira a LTS atual do Java e a versão estável atual do Spring Boot e do Spring Modulith antes de fixá-las.
- **Sem Docker (regra arquitetural 8)**: nenhuma etapa obrigatória pode exigir Docker, container ou orquestrador. Em desenvolvimento e teste o PostgreSQL é embarcado pela aplicação (`io.zonky.test:embedded-postgres`); em produção é o do sistema operacional. Não reintroduza Testcontainers nem `docker-compose.yml`.
- **Paridade Windows/Linux**: todo script e comando precisa funcionar nos dois. Nada de caminho absoluto no estilo `/var/...`; diretórios vêm de configuração. O que é acionável por linha de comando tem versão `.sh` e `.ps1` em `scripts/`.
- **Execução**: a aplicação sobe por um script único (`scripts/start.sh`, `scripts/start.ps1`), que também sobe o banco. Ao acrescentar um componente obrigatório, inclua-o nesse script ou justifique por escrito.

## Convenções

- Pacote base: `io.github.rogeriocisi.airlab`; módulos `study`, `allocation`, `session`, `redaction`, `scoring`, `unblinding`, `analysis`, `participant`, `audit`, `catalog`, `trials`.
- Código, identificadores e commits em **inglês**; documentação e interface em **português**.
- Commits pequenos, no padrão Conventional Commits.
- Vocabulário neutro (P3): *reading*, *item*, *target*, *decoy*, *score*. Nunca linguagem que pressuponha a origem da informação.
- **Consulente, não *sitter***: em português — documentação, interface, comentários em português — quem avalia as leituras é o **consulente**. A forma inglesa *sitter* fica registrada no glossário da SPEC, por ser o termo da literatura, e permanece nos identificadores do código (`sitterCode`, `sitter_code`, `ParticipantRole.SITTER`), pela regra de código em inglês.

## Estado e ordem do MVP

Já implementado (ver `docs/PLAN.md`, seção 2, para a tabela completa):

1. Plano técnico, migrações `V1`–`V15` dos esquemas `research`/`identity`/`audit` e papel `airlab_app`
2. `audit`: trilha append-only encadeada por hash + CLI `airlab verify`
3. `allocation`: sorteio reproduzível (`SecureRandom` para a semente, SHA-256 em modo contador para a expansão), compromisso → revelação, custódia cifrada da semente
4. `study` (E1) e `participant` (E2), com pareamento
5. Partes puras de E5 (`ClueScrubber`) e E7 (Wilcoxon pareado exato como medida primária, binomial exato, permutação de Pratt e Birge, `PowerCheck`)
6. Escala de itens de cinco categorias com justificativa obrigatória (E6) e desvios de protocolo append-only (E1/E7)
7. `catalog` (SPEC 1.1) e `trials` (E11, E12): seção de testes no site e os dois protocolos de ESP integralmente automatizados, com semente comprometida, alvo derivado no travamento e impressão travada antes das candidatas

A fazer, nesta ordem: E4 (sessões), E5 (persistência da redação), E6 (avaliação cega), E7 (desmascaramento e relatório), E8/E9 (carimbo de tempo e pacote de verificação).

A suíte da matriz de cegamento cresce junto: cada linha nova da matriz da SPEC, seção 4, entra como teste antes da tela correspondente.

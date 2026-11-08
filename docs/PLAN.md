# Plano técnico do MVP — Crivo (AIR Lab)

> Derivado de `CONSTITUTION.md` (prevalece) e de `SPEC.md`. Versão 1.0 · novembro/2026.
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
| `analysis` | E7 | Parcial: teste binomial exato (análise primária). Persistência e relatório pendentes |
| `session` | E4 | Pendente (tabelas prontas) |
| `scoring` | E6 | Pendente (tabelas prontas) |
| `unblinding` | E7 | Pendente (tabelas prontas) |
| `cli` | E8 | **Implementado**: `airlab verify` |
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

**Garantias no banco, não só no código**: os triggers alcançam qualquer papel, inclusive
superusuário, de modo que a imposição vale também no perfil local, onde a aplicação conecta
como proprietária do banco.

## 4. Decisões criptográficas

| Decisão | Por quê |
|---|---|
| Hash da trilha sobre JSON canônico, **sem o número de sequência** | O auditor precisa recomputar os hashes a partir do pacote exportado, sem saber como as linhas foram numeradas |
| Semente de 32 bytes do `SecureRandom` | P5, imprevisibilidade |
| Expansão da semente por **SHA-256 em modo contador** (`SeedStream`), não por `SecureRandom` | Nenhum algoritmo de `SecureRandom` garante o mesmo resultado em outra JVM ou outro sistema; o sorteio tem de ser reproduzível por terceiros |
| Um domínio de expansão por decisão (`medium:`, `reading-order:`, `experimenter:`, `presentation:`) | Acrescentar uma decisão no futuro não desloca as decisões anteriores |
| Semente cifrada em custódia (AES-256-GCM) até o desmascaramento | Um dump do banco durante o estudo não revela a semente, e a semente sobrevive a um reinício |
| Campos identificáveis cifrados com AES-256-GCM, envelope `v1:` | LGPD (P6) e espaço para rotação de chave |

## 5. Dependências novas e sua justificativa (P8)

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

## 6. Testes

| Teste | Princípio | Tipo |
|---|---|---|
| `AuditTrailIntegrationTest` | P4 | PostgreSQL real: cadeia verificável, UPDATE/DELETE recusados, linha forjada detectada |
| `PreregistrationIntegrationTest` | P2, P5 | PostgreSQL real: congelamento, imutabilidade, emenda, sorteio único, revelação da semente |
| `AllocationEngineTest` | P5 | Unitário: reprodutibilidade, independência da ordem de entrada, compromisso ligado às alocações |
| `BinomialTestTest` | P9 | Unitário: valores de referência exatos (conferem com `binom.test` do R) |
| `ClueScrubberTest` | P1 | Unitário: primeiro nome, pistas de sessão, datas, item com mais de uma afirmação |
| `BlindingMatrixTest` | P1 | Estrutural: campo proibido ausente da projeção, não filtrado na view |
| `ModularityTest` | Regra 1 e 2 | Fronteiras de módulo |

Os testes de integração usam o mesmo PostgreSQL embarcado, então `./mvnw test` funciona em
Windows e Linux sem Docker e sem banco instalado.

## 7. Armadilhas conhecidas

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

## 8. Próximos cortes

1. **E4 (sessões)**: consumir `AllocationDrawn`, criar sessões, tela do experimentador com
   apenas o primeiro nome, upload de mídia com identificador aleatório.
2. **E5 (redação)**: persistência das leituras redigidas e itens, usando o `ClueScrubber` como
   validação de submissão.
3. **E6 (avaliação cega)**: links de uso único e expiráveis, formulário do sitter, bloqueio no
   envio, mais as linhas da matriz de cegamento que dependem de estado persistido.
4. **E7 (desmascaramento e análise)**: operação irreversível depois do fechamento de todas as
   avaliações, execução da análise primária, relatório neutro.
5. **E8/E9**: carimbo de tempo RFC 3161, pacote de verificação e script de análise em R.

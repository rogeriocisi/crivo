# Constitution — Crivo (codinome AIR Lab)

> Este documento define os **princípios inegociáveis** do projeto e a **arquitetura tecnológica de referência**. Toda spec, plano e tarefa deve estar em conformidade com ele. Quando houver conflito entre este documento e qualquer outro, **este prevalece**.
>
> Versão 1.2.0 · Ratificada em: novembro/2026 · 1.1.0: adicionada a seção "Implantação por fase" · 1.1.1: nome público "Crivo", codinome técnico "AIR Lab" · 1.2.0: arquitetura sem Docker — aplicação executada por linha de comando (JAR/Spring Boot) em Windows e Linux, com PostgreSQL nativo ou embarcado

---

## Parte I — Princípios

### P1. O cegamento é um invariante do sistema, não uma convenção
Nenhum usuário pode acessar uma informação que quebre o cegamento do seu papel, nem por erro de tela, nem por API, nem por exportação, log, nome de arquivo ou metadado.
- A autorização é aplicada na **camada de domínio e de dados**, nunca apenas na interface.
- Cada regra de cegamento tem **teste automatizado** que tenta violá-la.
- Uma quebra de cegamento é tratada como **incidente**: ela é registrada na trilha de auditoria e marca as sessões afetadas como comprometidas. Isso nunca é silenciado.

### P2. Pré-registro antes do dado
Hipóteses, critérios de inclusão, plano de análise e alocações sorteadas são **congelados e publicados como compromisso criptográfico (hash) antes da primeira sessão**.
- Depois de congelado, o pré-registro é imutável. Emendas geram uma nova versão, com data e justificativa, e a versão original continua visível.
- Uma análise que não esteja no pré-registro é rotulada como **exploratória** em todo relatório.

### P3. Neutralidade de resultado
O sistema não pressupõe a existência nem a inexistência do fenômeno.
- Resultados nulos, positivos e negativos usam **o mesmo relatório e o mesmo destaque**.
- A interface nunca usa linguagem que pressuponha a origem da informação ("o espírito disse"). O vocabulário é: *leitura*, *item*, *alvo*, *chamariz*, *pontuação*.

### P4. Trilha de auditoria imutável
Todo evento relevante (criação, sorteio, sessão, redação, avaliação, desmascaramento, emenda, incidente) é gravado num log **append-only, encadeado por hash** (cada registro contém o hash do anterior).
- Nenhuma operação de `UPDATE` ou `DELETE` é permitida na tabela de eventos (imposto por permissão de banco, não só por código).
- A cadeia pode ser verificada por uma ferramenta de linha de comando incluída no projeto.

### P5. Aleatoriedade verificável
Todo sorteio usa gerador criptograficamente seguro (`SecureRandom`) e segue o esquema **compromisso → revelação**:
1. Gera-se uma semente aleatória.
2. Publica-se `SHA-256(semente + alocações)` no pré-registro.
3. Ao final do estudo, revela-se a semente; qualquer pessoa pode refazer o sorteio e conferir.

### P6. Privacidade e ética por padrão
- **LGPD**: dados sobre convicção religiosa, saúde e informações sobre falecidos são tratados como **sensíveis**. Valem minimização, pseudonimização (o participante é identificado por código nos dados de pesquisa), criptografia em repouso para campos identificáveis e consentimento específico e destacado.
- Os dados identificáveis ficam em **esquema separado** dos dados de pesquisa.
- **Pessoas enlutadas**: linguagem cuidadosa, possibilidade de desistência a qualquer momento sem justificativa e devolutiva planejada.
- Se o estudo tiver fins de publicação acadêmica, o protocolo passa por **Comitê de Ética em Pesquisa (CEP/Plataforma Brasil)** antes da coleta com participantes reais.

### P7. Dados abertos e reprodutibilidade
- Todo estudo concluído pode gerar um **pacote de verificação**: pré-registro, sementes, alocações, avaliações anonimizadas, trilha de auditoria e script de análise.
- A análise do relatório oficial é **reexecutável** a partir desse pacote, com o mesmo resultado numérico.
- O código-fonte é aberto.

### P8. Simplicidade sustentável
O projeto é mantido por um desenvolvedor solo.
- Monólito modular. Nada de microsserviços, mensageria distribuída ou Kubernetes sem justificativa escrita.
- Toda dependência nova precisa justificar o seu custo de manutenção.
- Preferir tecnologias maduras e chatas a tecnologias novas e brilhantes.

### P9. Testes como prova
- O núcleo de domínio (protocolos, sorteio, cegamento, estatística) tem cobertura de testes alta e é testado **antes** da interface.
- Funções estatísticas são validadas contra valores de referência conhecidos (por exemplo, resultados calculados em R).
- Testes de cegamento (P1) e de integridade da trilha (P4) **nunca** podem ser desativados ou ignorados para fazer o build passar.

### P10. IA como ferramenta periférica, nunca como juiz
- Nenhum modelo de IA participa da avaliação de leituras, da pontuação nem da decisão de acerto.
- Uso permitido: transcrição de áudio **local** (os dados não saem da máquina) e assistência ao desenvolvimento.

---

## Parte II — Arquitetura tecnológica de referência

### Visão geral

```
┌──────────────────────────────────────────────────────────────┐
│               Crivo / AIR Lab (monólito modular)             │
│                                                              │
│  Web (Thymeleaf + HTMX)   │   API REST (exportação/auditor)  │
│ ─────────────────────────────────────────────────────────────│
│  Módulos de domínio (Spring Modulith)                        │
│   ├─ study        (estudo, pré-registro, emendas)            │
│   ├─ allocation   (sorteio, compromisso/revelação)           │
│   ├─ session      (agenda, sessões, gravação, transcrição)   │
│   ├─ redaction    (lista de itens, remoção de pistas)        │
│   ├─ scoring      (avaliação cega)                           │
│   ├─ unblinding   (desmascaramento controlado)               │
│   ├─ analysis     (estatística, relatório)                   │
│   ├─ participant  (pessoas, consentimento — dados sensíveis) │
│   └─ audit        (trilha encadeada por hash)                │
│ ─────────────────────────────────────────────────────────────│
│  PostgreSQL                                                  │
│   ├─ schema research   (pseudonimizado)                      │
│   ├─ schema identity   (identificável, criptografado)        │
│   └─ schema audit      (append-only)                         │
└──────────────────────────────────────────────────────────────┘
          │                                  │
   Armazenamento de arquivos          Carimbo de tempo externo
   (áudios/transcrições, criptografado)  (RFC 3161 / OpenTimestamps)
```

### Stack sugerida

| Camada | Escolha | Por quê |
|---|---|---|
| Linguagem | **Java (LTS mais recente)** | Domínio do autor, tipagem forte, ecossistema maduro |
| Framework | **Spring Boot** (versão estável atual) | Padrão de mercado; segurança, dados e testes integrados |
| Modularidade | **Spring Modulith** | Fronteiras de módulo verificadas por teste, eventos de domínio |
| Build | **Maven** | Simples e previsível (Gradle é aceitável se preferido) |
| Banco | **PostgreSQL** instalado nativamente (serviço do SO) em produção; **embarcado pela aplicação** em desenvolvimento e teste | Permissões por esquema e por papel; garante o append-only (P4). Não é substituível por H2: o P4 depende de `REVOKE`/papéis do PostgreSQL |
| Migrações | **Flyway** | Versionamento do esquema auditável |
| Acesso a dados | Spring Data JPA nos agregados + **jOOQ ou JDBC** para consultas estatísticas | JPA no CRUD; SQL explícito onde a precisão importa |
| Interface | **Thymeleaf + HTMX** | Sem SPA para manter; renderizado no servidor; acessível |
| Segurança | **Spring Security** | Login com 2FA para pesquisadores; links de uso único e expiráveis para consulentes e avaliadores |
| Estatística | **Apache Commons Statistics / Commons Math** + script de referência em **R** no pacote de verificação | Cálculo no app e validação cruzada independente |
| Criptografia | JCA (`SecureRandom`, SHA-256) + criptografia de campo para dados identificáveis | Sem dependência exótica |
| Carimbo de tempo | **RFC 3161 (TSA)** e/ou **OpenTimestamps**; pré-registro também espelhado no **OSF Registries** | Prova de anterioridade verificável por terceiros |
| Transcrição (opcional) | **whisper.cpp** local | Os dados de participantes não saem da máquina (P6, P10) |
| Testes | JUnit 5, AssertJ, **PostgreSQL embarcado** (`io.zonky.test:embedded-postgres`, binários reais do PostgreSQL, sem Docker), ArchUnit/Modulith tests | Testes de cegamento contra PostgreSQL real, rodando em Windows e Linux sem container |
| Execução | **JAR executável do Spring Boot**, acionado por linha de comando (`java -jar`) em Windows e Linux, via script único `scripts/start.sh` no Linux e `scripts\start.cmd` no Windows | Um comando sobe tudo; sem Docker, sem runtime de container |
| Implantação | **Processo do sistema operacional**: unidade `systemd` no Linux, serviço do Windows (WinSW/`sc.exe`) ou Agendador de Tarefas | Operável por uma pessoa, sem camada de orquestração |
| Backups | Dump criptografado diário + teste de restauração mensal | Dados de pesquisa são irreproduzíveis |
| Observabilidade | Logs estruturados **sem dados pessoais** + Spring Actuator | Logs nunca podem quebrar cegamento nem privacidade |

### Regras arquiteturais

1. **Fronteiras de módulo**: um módulo só acessa outro pela API pública dele ou por eventos de domínio. Isso é verificado em teste (Spring Modulith/ArchUnit).
2. **O módulo `participant` é o único** que lê o esquema `identity`. Os demais trabalham só com códigos pseudônimos.
3. **Projeções por papel**: cada papel tem DTOs/consultas próprias que, por construção, não contêm campos proibidos para aquele papel. Nunca se filtra campo na view.
4. **Eventos de domínio** alimentam a trilha de auditoria. Uma operação de domínio sem evento de auditoria é considerada bug.
5. **O desmascaramento** é uma operação explícita, irreversível, autorizada só depois do fechamento de todas as avaliações, e registrada na trilha.
6. **Configuração por ambiente**: segredos ficam fora do repositório (variáveis de ambiente/arquivo `.env` não versionado).
7. **Nomes de arquivos e metadados** de mídia são substituídos por identificadores aleatórios no upload (P1).
8. **Sem dependência de Docker**: nenhuma etapa obrigatória do projeto (build, teste, execução, implantação, verificação do pacote de auditoria) pode exigir Docker, container ou orquestrador. Quem clona o repositório precisa apenas de um JDK e do script de inicialização.
9. **Processo único**: a aplicação é um só processo Java, com o servidor web embutido. Componentes externos obrigatórios se limitam ao PostgreSQL; qualquer novo processo obrigatório exige justificativa escrita (P8).
10. **Paridade Windows/Linux**: todo caminho, script e comando documentado funciona nos dois sistemas. Nada de caminho absoluto no estilo `/var/...` no código; diretórios de dados vêm de configuração (`crivo.storage.path`), com padrão relativo ao diretório de trabalho.

### Implantação por fase

O mesmo artefato (um JAR executável) serve a todas as fases, acionado pelo mesmo script de linha de comando. O que muda é **onde** roda, **qual perfil** está ativo e **que dados** existem ali.

| Fase | Onde roda | Dados | Exposição |
|---|---|---|---|
| **Desenvolvimento** | Workstation do autor, **Windows ou Linux** (perfil `local`) | Somente fictícios | Local (`localhost`) |
| **Dry-run** | Workstation Windows ou Linux (perfil `local`) | Fictícios (pessoas reais simulando papéis com falecidos inventados) | Temporária, via **Cloudflare Tunnel** ou **Tailscale Funnel**; sem abrir portas no roteador; túnel desligado ao fim de cada sessão de teste |
| **Piloto / estudos reais** | **VPS Linux com região em São Paulo** (2 vCPU, 2–4 GB RAM são suficientes), perfil `prod`: JAR como serviço `systemd` + PostgreSQL do sistema | Reais, sensíveis | HTTPS público, só com as rotas da aplicação |
| **Pós-estudo** | VPS desligada ou arquivada | Dados identificáveis eliminados conforme o termo de consentimento | Nenhuma; o que permanece público fica nos repositórios externos (abaixo) |

#### Regras de implantação

1. **Residência de dados**: dados reais de participantes ficam em infraestrutura com região no Brasil. Transferência internacional só com base legal documentada (LGPD, art. 33).
2. **Provedor**: preferir VPS simples (uma máquina, pacotes do sistema operacional) a serviços gerenciados ou plataformas de container, por causa do P8. Candidatos com região em São Paulo: AWS, Google Cloud, Azure, Oracle Cloud, Vultr, Magalu Cloud. Confirme preço e condições atuais na hora da escolha.
3. **Borda**: proxy reverso **Caddy** instalado como pacote/serviço do sistema (binário único, HTTPS automático), encaminhando para a aplicação em `127.0.0.1:8080`. Só as portas 80/443 ficam abertas; o SSH usa chave e, de preferência, só é acessível via Tailscale.
4. **Banco**: em produção, o PostgreSQL é o do sistema operacional (`apt install postgresql`), escutando **somente** em `127.0.0.1`, nunca exposto à internet. Em desenvolvimento e teste, a própria aplicação sobe uma instância embarcada (binários reais do PostgreSQL) em `~/.airlab/pgdata`, descartável e isolada — fora do repositório e fora de pasta sincronizada. Nos dois casos valem os mesmos esquemas, papéis e migrações Flyway — nada de banco diferente por ambiente.
5. **Backups**: dump diário criptografado (por exemplo, `age` ou `restic`) enviado para **fora da VPS**, seja armazenamento de objetos de outro provedor, seja a workstation do autor. A restauração é testada mensalmente.
6. **Transcrição sempre local (P6, P10)**: o whisper.cpp roda na **workstation**, nunca na VPS. O fluxo é: baixar o áudio por canal autenticado → transcrever localmente → subir apenas o texto → apagar a cópia local do áudio ao fim do estudo, conforme o termo.
7. **Segredos**: em variáveis de ambiente ou arquivo `.env` com permissão restrita no servidor, nunca no repositório.
8. **Atualizações**: versões de JDK, PostgreSQL e dependências fixadas e registradas; atualizações de segurança do sistema operacional automáticas; atualização da aplicação só entre sessões de coleta, nunca durante um estudo em andamento sem registro na trilha de auditoria.

#### Execução por linha de comando

A aplicação é acionada por **um único script**, que sobe todos os componentes necessários (banco e aplicação) e vale para Windows e Linux:

| Sistema | Comando |
|---|---|
| Linux / macOS | `./scripts/start.sh` |
| Windows | `scripts\start.cmd` |
| Windows (chamando o PowerShell direto) | `.\scripts\start.ps1` |

Regras:

1. **Um comando, tudo de pé**: no perfil `local` (padrão), o script valida o JDK, constrói o JAR se preciso, sobe o PostgreSQL embarcado, aplica as migrações Flyway e inicia a aplicação. Nenhuma instalação manual de banco é exigida do desenvolvedor.
2. **Pré-requisito único**: um JDK (LTS) instalado. O Maven vem pelo *wrapper* do próprio repositório; o PostgreSQL de desenvolvimento vem como dependência da aplicação.
3. **Mesmo script em produção**: `--profile prod` usa o PostgreSQL do sistema, lê os segredos do ambiente (ou do `.env` não versionado) e não sobe banco embarcado; a falta de qualquer variável obrigatória interrompe a inicialização com mensagem clara, nunca com um padrão silencioso.
4. **CLI de auditoria**: a verificação do P4/P5 também é linha de comando e o mesmo artefato — `./scripts/airlab.sh verify` e `.\scripts\airlab.ps1 verify` — e precisa rodar nos dois sistemas operacionais, porque o auditor externo não escolhe a plataforma.
5. **Sem passo oculto**: qualquer etapa que o script não faça sozinho (criar papéis no PostgreSQL de produção, gerar segredos) está documentada no `README.md` como comando explícito.
6. **O caminho recomendado no Windows é o `.cmd`**, não o `.ps1`. A política de execução do PowerShell vem como `Restricted` numa instalação limpa e recusa script não assinado; o `start.cmd` chama o `start.ps1` com `-ExecutionPolicy Bypass` e não altera configuração nenhuma da máquina. Alterar a política de execução do usuário é decisão dele, nunca do projeto nem de quem o instala.
7. **Nada de `2>&1` em executável nativo dentro de script PowerShell.** Redirecionar a saída de erro de um `.exe` faz o PowerShell embrulhar cada linha num `ErrorRecord`, e com `$ErrorActionPreference = 'Stop'` isso aborta o script com um erro inventado — foi o que aconteceu com `java -version`, que escreve na saída de erro. Use a forma que escreve na saída padrão (`java --version`) ou capture sem redirecionar.

#### Parte pública (separada da aplicação)

O que precisa permanecer acessível depois do estudo **não depende do servidor da aplicação**:

| Artefato | Onde publicar |
|---|---|
| Pré-registro | **OSF Registries** (com o hash e o comprovante de carimbo de tempo) |
| Pacote de verificação | **Zenodo** (gera DOI e é citável) |
| Código-fonte | **GitHub** (repositório público) |
| Página de resultados | **GitHub Pages** (site estático) |

#### Alternativa institucional
Se o estudo for vinculado a uma universidade (por exemplo, por causa do CEP), avaliar a infraestrutura da própria instituição para a fase de piloto. Isso reforça a credibilidade e pode cumprir exigências do comitê. Nesse caso, as regras 1 a 8 continuam valendo.

---

## Parte III — Fluxo de desenvolvimento

- **Spec-driven**: Constitution → Spec → Plano técnico → Tarefas → Implementação. Nenhuma funcionalidade sem spec correspondente.
- **Commits pequenos** e mensagens no padrão Conventional Commits.
- **Definição de pronto**: testes passando (incluindo cegamento e auditoria), migração revisada, documentação atualizada e nenhum dado pessoal em logs.
- **Idioma**: documentação e interface em português; código, identificadores e commits em inglês.
- **Nomenclatura**: o nome público é **Crivo**; o codinome técnico **AIR Lab** é usado em pacotes Java (`io.github.rogeriocisi.airlab`), artefatos Maven e na CLI (`airlab`).

## Parte IV — Governança

- Esta constitution só é alterada por decisão explícita do pesquisador principal, com nota de versão (SemVer: MAJOR para princípio removido ou alterado, MINOR para princípio novo, PATCH para redação).
- Toda revisão de código ou plano verifica a conformidade com P1–P10. Uma violação bloqueia o merge.
- Em caso de dúvida entre velocidade e rigor, **o rigor vence**: a razão de existir do projeto é a credibilidade.

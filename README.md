# Crivo — protocolos cegos para pesquisa de cognição anômala

> Codinome técnico: **AIR Lab** (*Anomalous Information Reception*, ou recepção anômala de informação), herdado do primeiro protocolo

O Crivo é uma plataforma para conduzir estudos de **cognição anômala** com **cegamento múltiplo, sorteio verificável, pré-registro imutável e trilha de auditoria encadeada por hash**. Tudo foi pensado para que qualquer pessoa, inclusive um auditor cético, consiga conferir que o protocolo foi seguido.

A mediunidade é um ramo da plataforma — na literatura, *recepção anômala de informação*, ou AIR. Os testes de percepção extrassensorial são outro. A distinção é mantida de propósito: AIR designa informação sobre pessoa falecida, e chamar um teste de precognição de AIR seria impreciso.

## Catálogo de protocolos

O catálogo tem duas seções: **protocolos AIR**, com o automatizado em destaque, e **experimentos extras**.

| Seção | Código | Protocolo | Acaso | Automação |
|---|---|---|---|---|
| Protocolos AIR | **P4** | [Leitura estruturada automatizada](docs/SPEC.md#e13-leitura-estruturada-automatizada-p4) | 50% | **Integral**: conta, pedido, leitura, conferência e avisos sem ninguém da equipe |
| Protocolos AIR | **P1** | [Leitura pareada cega](docs/SPEC.md) | 50% | Parcial: exige equipe — sessão síncrona com experimentador *proxy* e redator |
| Experimentos extras | **P2** | Escolha forçada precognitiva (ESP) | 25% | Integral — qualquer pessoa faz pelo navegador, sem conta |
| Experimentos extras | **P3** | Visão remota com quatro alvos (ESP) | 25% | Integral — qualquer pessoa faz pelo navegador, sem conta |

Todos herdam as mesmas garantias, e cada um declara na própria página **o que o seu resultado não sustenta**. Os dois testes de ESP não dizem nada sobre mediunidade: eles estão ali porque são integralmente verificáveis por quem os faz. Em `/testes`, no site.

### O AIR automatizado (P4), em uma frase

O consulente informa o primeiro nome da pessoa falecida e responde a um questionário fechado sobre ela; um médium sorteado recebe só o primeiro nome e responde ao mesmo questionário; o sistema confere campo a campo contra o alvo verdadeiro e contra um chamariz de perfil parecido — **ninguém julga se uma afirmação "encaixa"**. A especificação completa, com a matriz de visibilidade e os limites declarados, está no épico [E13 da SPEC](docs/SPEC.md#e13-leitura-estruturada-automatizada-p4).

### Como participar do P4: conta, perfil e telas

1. **Crie uma conta** em `/conta/criar`: nome, e-mail, senha e aceite do termo.
2. **Escolha o perfil** — **consulente** (faz o pedido de consulta) ou **médium** (responde aos pedidos). O perfil é fixo: o banco recusa a troca.
3. **Confirme o e-mail** pelo link, que vale por 48 horas. No perfil local nenhum e-mail sai da máquina: o link aparece no log da aplicação (`Perfil local: link de confirmacao da conta …`) e na caixa de saída do administrador.
4. **Entre** em `/login` com e-mail e senha; cada perfil cai no seu painel.

| Perfil | Telas |
|---|---|
| **Consulente** | Painel com a situação do pedido · Novo pedido (questionário) · Devolutiva depois do fim do estudo · Minha conta (dados, termo, excluir conta) |
| **Médium** | Painel com pedidos recebidos e prazos · Leitura de cada nome do par, com revisão antes de travar · Minha conta |
| **Administrador** | Painel com contagens — nunca notas — e situação da instalação · **Estudo P4**: pré-registro, congelamento e progresso · Avisos de cada etapa concluída · Caixa de saída (perfil local) · Sinalizações (fase seguinte) |

O administrador **não é um perfil escolhível**: é a conta do pesquisador, definida na configuração do servidor (ver *Entrar na aplicação*).

**Estado:** as fases A, B e C do E13 estão implementadas. Contas com perfil fixo, confirmação por e-mail, painéis por perfil e avisos ao administrador; o **pedido de consulta**, com o questionário (versão `v1-rascunho`, ainda em revisão) travado e só o compromisso salgado na trilha; e o **fluxo até a leitura**: o administrador congela o estudo P4, e um ciclo do servidor, a cada minuto, forma os pares, sorteia o médium, libera os dois primeiros nomes, controla o prazo e reenvia o que vencer. O médium responde, revê e trava cada leitura. A conferência automática, o desmascaramento no N e a devolutiva (fase D) vêm a seguir.

**Para experimentar no perfil local:** crie uma conta de médium e duas de consulente e faça dois pedidos com o mesmo gênero, a mesma faixa etária e o mesmo período do falecimento. Depois entre como administrador, abra **Estudo P4** e congele o pré-registro. Em até um minuto, o médium vê o pedido no painel.

O Crivo não existe para provar que a mediunidade é real, nem que não é. Ele existe para que **a resposta, qualquer que seja, seja defensável**.

> *"Passar pelo crivo da razão."*

## Status

🧱 **Fase 0 → 1: fundamentos implementados.** O núcleo de confiança está de pé: trilha de auditoria encadeada por hash, sorteio reproduzível com compromisso → revelação, pré-registro congelável e CLI de verificação. No AIR automatizado (P4), as fases A, B e C estão no ar: contas com perfil fixo, pedido de consulta com compromisso salgado, estudo P4 congelado, pareamento e sorteio do médium automáticos, leitura travada e prazos com reenvio; a conferência automática e a devolutiva vêm a seguir. Na leitura pareada (P1), os épicos de campo (sessões, redação, avaliação cega, desmascaramento) estão com o esquema de banco pronto e a implementação pendente. Ver a tabela de estado em [`docs/PLAN.md`](docs/PLAN.md).

## Como rodar

**Pré-requisito único: um JDK 21 ou mais novo.** Não é preciso Docker, nem instalar PostgreSQL, nem Maven: o banco de desenvolvimento é embarcado pela própria aplicação e o Maven vem pelo wrapper do repositório.

Linux ou macOS:

```bash
./scripts/start.sh
```

Windows (PowerShell ou prompt de comando) — este é o caminho recomendado, e funciona em máquina recém-instalada:

```bat
scripts\start.cmd
```

Windows, chamando o script do PowerShell direto:

```powershell
.\scripts\start.ps1
```

Esta última forma depende da política de execução do seu usuário, e por padrão o Windows recusa script não assinado. O `start.cmd` existe justamente para isso: ele chama o `start.ps1` com `-ExecutionPolicy Bypass`, sem mexer em configuração nenhuma da máquina.

**Atenção a um detalhe que pega quem clona o repositório numa pasta sincronizada** (Google Drive, OneDrive, ou um compartilhamento de rede): `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` **não resolve** nesse caso. O Windows classifica os arquivos dessas unidades na zona *Internet*, e `RemoteSigned` exige assinatura digital justamente para essa zona. `Unblock-File` também não ajuda: essas unidades não guardam *alternate data streams*, então não há marca para remover — a zona vem da unidade, não do arquivo. Verifique o seu caso com:

```powershell
[System.Security.Policy.Zone]::CreateFromUrl('G:\caminho\do\repo\scripts\start.ps1').SecurityZone
```

Se o resultado for `Internet`, as opções são: usar o `scripts\start.cmd` (recomendado), chamar o script com `powershell -ExecutionPolicy Bypass -File .\scripts\start.ps1`, ou manter o repositório num disco local. Esta última é a melhor das três por outros dois motivos: o `target/` com um JAR de ~150 MB deixa de ser sincronizado a cada build, e o PostgreSQL de desenvolvimento nunca deve ter o diretório de dados em pasta sincronizada.

O script valida o JDK, constrói o JAR, sobe o PostgreSQL embarcado em `~/.airlab/pgdata`, aplica as migrações e inicia a aplicação em <http://127.0.0.1:8080>. `Ctrl+C` encerra tudo.

### No navegador, pelo GitHub Codespaces

Caminho opcional, para quem não quer instalar nada. O script único continua sendo a forma de rodar, e nenhuma etapa depende de container. No GitHub, abra **Code → Codespaces → Create codespace on main**. A configuração em `.devcontainer/` já traz o JDK 21 e baixa as dependências. Quando o terminal estiver pronto:

```bash
./scripts/start.sh
```

O Codespaces encaminha a porta 8080 e abre a aplicação numa aba. Se a aba não abrir, ela fica na guia **Ports**. A senha do `pesquisador` sai no log, como no perfil local (ver abaixo).

### Entrar na aplicação

A entrada em `/login` serve a três perfis. **Consulentes e médiuns** do P4 entram com e-mail e senha da conta que criaram. **O administrador** entra com o usuário da configuração:

- **Usuário**: `pesquisador`.
- **Senha, no perfil local**: gerada a cada execução e impressa uma vez no log, numa linha de aviso `Perfil local: usuario "pesquisador" com senha temporaria desta execucao: …`. Nada fica salvo entre execuções, e nenhuma credencial vai para o repositório.
- **Para fixar a sua**: defina `AIRLAB_RESEARCHER_USER` e `AIRLAB_RESEARCHER_PASSWORD_HASH` (hash bcrypt) no `.env`, que não é versionado.
- **Em produção**: as duas variáveis são obrigatórias; sem elas a aplicação não inicia.

Opções úteis: `--profile prod` (`-AppProfile prod` no PowerShell), `--port 9090`, `--rebuild` (build com a suíte de testes), `--skip-build`.

### Verificação (auditoria)

```bash
./scripts/airlab.sh verify
```

```powershell
.\scripts\airlab.ps1 verify
```

Confere a cadeia de hashes da trilha, o hash de cada versão do pré-registro e, para os estudos já desmascarados, reproduz o sorteio a partir da semente. Sai com código 1 se algo não confere.

### Testes

```bash
./mvnw test
```

Os testes de cegamento e de integridade da trilha rodam contra um PostgreSQL real, embarcado, em Windows e Linux. Eles nunca podem ser desativados para fazer o build passar (Constitution P1, P4, P9).

### Produção

Perfil `prod`: PostgreSQL do sistema operacional escutando só em `127.0.0.1`, aplicação como serviço (`systemd` no Linux, serviço do Windows), Caddy na borda com HTTPS automático. Os segredos vêm do ambiente ou de um `.env` não versionado — ver [`.env.example`](.env.example). As regras completas estão na Constitution, em *Implantação por fase*.

**Inscrição do P4 em produção.** Fica fechada até que existam envio de e-mail e um contato humano (SPEC E10, travas 3 e 5):

| Variável | Para quê |
|---|---|
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Servidor SMTP que envia a confirmação de conta e os avisos |
| `AIRLAB_MAIL_FROM` | Remetente dos e-mails |
| `AIRLAB_CONTACT_EMAIL` | Contato humano mostrado na página de inscrição |
| `AIRLAB_ADMIN_EMAIL` | Opcional: quem recebe por e-mail os avisos de etapa concluída (sempre aparecem no painel) |
| `AIRLAB_CEP_REFERENCE` | Referência da aprovação do comitê de ética. Sem ela, o formulário de pedido de consulta fica fechado em produção |
| `AIRLAB_BASE_URL` | Endereço público do site (por exemplo `https://crivo.exemplo.org`), para o link do e-mail que avisa o médium de um pedido novo |

Antes de abrir a inscrição a pessoas reais, vale a trava 4 do E10: aprovação do comitê de ética (CEP/Plataforma Brasil). O pedido de consulta, que coleta os dados sensíveis, só abre em produção com `AIRLAB_CEP_REFERENCE` definida. E o estudo P4 só congela, em produção, um questionário revisado: a versão `v1-rascunho` vale só no perfil local.

## Contribuir

```bash
git clone https://github.com/rogeriocisi/crivo.git
```

Faça o *fork* pelo GitHub, trabalhe num ramo, rode `./mvnw test` e abra o *pull request*. Commits pequenos, no padrão Conventional Commits, e nenhuma funcionalidade sem a especificação correspondente — as regras estão no [`CLAUDE.md`](CLAUDE.md). Objeções metodológicas valem tanto quanto código: abra uma [issue](https://github.com/rogeriocisi/crivo/issues).

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/VISION.md`](docs/VISION.md) | Visão, problema, objetivos, não objetivos, papéis, roadmap e riscos |
| [`docs/CONSTITUTION.md`](docs/CONSTITUTION.md) | Princípios inegociáveis (P1–P10), arquitetura de referência e implantação por fase |
| [`docs/SPEC.md`](docs/SPEC.md) | Especificação macro: o que é uma leitura, protocolo pareado, matriz de cegamento, épicos, modelo de domínio e MVP |
| [`docs/PLAN.md`](docs/PLAN.md) | Plano técnico do MVP: módulos, migrações, decisões criptográficas, dependências e estado por épico |
| [`docs/REFERENCES.md`](docs/REFERENCES.md) | Papers que embasam o projeto (a favor e críticos) e a decisão de projeto que cada um informa |

Em caso de conflito entre documentos, a **Constitution prevalece**.

**Artigo técnico** (rascunho, em português): [`paper/artigo-tecnico.pt-BR.md`](paper/artigo-tecnico.pt-BR.md)

## Stack

Java 21 (LTS) · Spring Boot · Spring Modulith · Spring Boot Mail · PostgreSQL (embarcado em desenvolvimento, do sistema em produção) · Flyway · Thymeleaf + HTMX · JUnit 5 · Maven Wrapper

Sem Docker: todo passo do projeto — build, teste, execução, implantação e verificação — roda por linha de comando em Windows e Linux com um JDK instalado.

## Próximos passos

1. P4, fase D (E13): desmascaramento automático no N, conferência, análise primária, relatório e devolutiva
2. E4 (sessões) e E5 (redação), consumindo o evento do sorteio
3. E6 (avaliação cega) com links de uso único
4. E7 (desmascaramento e análise primária) e o relatório neutro
5. Carimbo de tempo RFC 3161 e pacote de verificação (E8/E9)

## Autor

Rogerio Cisi

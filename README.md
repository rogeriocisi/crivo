# Crivo — protocolos cegos para pesquisa de mediunidade

> Codinome técnico: **AIR Lab** (*Anomalous Information Reception*, ou recepção anômala de informação)

O Crivo é uma plataforma para conduzir estudos de mediunidade com **cegamento múltiplo, sorteio verificável, pré-registro imutável e trilha de auditoria encadeada por hash**. Tudo foi pensado para que qualquer pessoa, inclusive um auditor cético, consiga conferir que o protocolo foi seguido.

O Crivo não existe para provar que a mediunidade é real, nem que não é. Ele existe para que **a resposta, qualquer que seja, seja defensável**.

> *"Passar pelo crivo da razão."*

## Status

🧱 **Fase 0 → 1: fundamentos implementados.** O núcleo de confiança está de pé: trilha de auditoria encadeada por hash, sorteio reproduzível com compromisso → revelação, pré-registro congelável e CLI de verificação. Os épicos de campo (sessões, redação, avaliação cega, desmascaramento) estão com o esquema de banco pronto e a implementação pendente. Ver a tabela de estado em [`docs/PLAN.md`](docs/PLAN.md).

## Como rodar

**Pré-requisito único: um JDK 21 ou mais novo.** Não é preciso Docker, nem instalar PostgreSQL, nem Maven: o banco de desenvolvimento é embarcado pela própria aplicação e o Maven vem pelo wrapper do repositório.

Linux ou macOS:

```bash
./scripts/start.sh
```

Windows (PowerShell):

```powershell
.\scripts\start.ps1
```

Windows (prompt de comando):

```bat
scripts\start.cmd
```

O script valida o JDK, constrói o JAR, sobe o PostgreSQL embarcado em `~/.airlab/pgdata`, aplica as migrações e inicia a aplicação em <http://127.0.0.1:8080>. `Ctrl+C` encerra tudo.

### Entrar na aplicação

A entrada em `/login` é para quem **conduz** o estudo, não para participantes — estes recebem link de uso único e nunca têm conta. Hoje ela ainda não leva a telas de pesquisador: a conta existe para proteger o que vem a seguir.

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

Java 21 (LTS) · Spring Boot · Spring Modulith · PostgreSQL (embarcado em desenvolvimento, do sistema em produção) · Flyway · Thymeleaf + HTMX · JUnit 5 · Maven Wrapper

Sem Docker: todo passo do projeto — build, teste, execução, implantação e verificação — roda por linha de comando em Windows e Linux com um JDK instalado.

## Próximos passos

1. E4 (sessões) e E5 (redação), consumindo o evento do sorteio
2. E6 (avaliação cega) com links de uso único
3. E7 (desmascaramento e análise primária) e o relatório neutro
4. Carimbo de tempo RFC 3161 e pacote de verificação (E8/E9)

## Autor

Rogerio Cisi

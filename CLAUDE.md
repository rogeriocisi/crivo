# CLAUDE.md — Crivo (AIR Lab)

Instruções para o Claude Code neste repositório.

## Leia antes de qualquer tarefa

1. `docs/CONSTITUTION.md`: princípios inegociáveis P1–P10, arquitetura e implantação. **Prevalece sobre todo o resto.**
2. `docs/SPEC.md`: especificação macro, com a matriz de cegamento (seção 4) e os épicos E1–E9.
3. `docs/VISION.md`: contexto, objetivos e não objetivos.

## Regras de trabalho

- **Spec-driven**: Constitution → Spec → Plano técnico → Tarefas → Implementação. Não implemente funcionalidade sem spec correspondente; se faltar, proponha a spec primeiro.
- **Antes de propor um plano ou código, verifique a conformidade com P1–P10.** Se algo violar um princípio, pare e aponte.
- **Cegamento (P1)**: toda regra da matriz da SPEC, seção 4, tem teste que tenta violá-la. Nunca desative, pule ou marque como `@Disabled` testes de cegamento ou de integridade da trilha de auditoria.
- **Auditoria (P4)**: toda operação de domínio emite um evento de auditoria. Operação sem evento é bug.
- **Privacidade (P6)**: nada de dados pessoais em logs, mensagens de exceção, nomes de arquivo ou fixtures com dados reais. Apenas o módulo `participant` acessa o esquema `identity`.
- **IA (P10)**: nenhum código usa IA para avaliar leituras ou pontuar.
- **Simplicidade (P8)**: monólito modular; justifique por escrito qualquer dependência nova.
- **Versões**: confira a LTS atual do Java e a versão estável atual do Spring Boot e do Spring Modulith antes de fixá-las.

## Convenções

- Pacote base: `io.github.rogeriocisi.airlab`; módulos `study`, `allocation`, `session`, `redaction`, `scoring`, `unblinding`, `analysis`, `participant`, `audit`.
- Código, identificadores e commits em **inglês**; documentação e interface em **português**.
- Commits pequenos, no padrão Conventional Commits.
- Vocabulário neutro (P3): *reading*, *item*, *target*, *decoy*, *score*. Nunca linguagem que pressuponha a origem da informação.

## Ordem sugerida do MVP

1. Plano técnico (estrutura de módulos, migrações Flyway iniciais, esquemas `research`/`identity`/`audit` e papéis do banco)
2. `audit`: trilha append-only encadeada por hash + CLI `airlab verify` (verificação da cadeia)
3. `allocation`: sorteio com `SecureRandom`, compromisso → revelação, reprodução pela CLI
4. Suíte de testes da matriz de cegamento (Testcontainers + PostgreSQL)
5. Demais épicos, conforme o corte do MVP na SPEC, seção 8

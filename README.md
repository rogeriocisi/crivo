# Crivo — protocolos cegos para pesquisa de mediunidade

> Codinome técnico: **AIR Lab** (*Anomalous Information Reception*, ou recepção anômala de informação)

O Crivo é uma plataforma para conduzir estudos de mediunidade com **cegamento múltiplo, sorteio verificável, pré-registro imutável e trilha de auditoria encadeada por hash**. Tudo foi pensado para que qualquer pessoa, inclusive um auditor cético, consiga conferir que o protocolo foi seguido.

O Crivo não existe para provar que a mediunidade é real, nem que não é. Ele existe para que **a resposta, qualquer que seja, seja defensável**.

> *"Passar pelo crivo da razão."*

## Status

📐 **Fase 0: fundamentos.** Por enquanto o repositório contém só a documentação de especificação. Ainda não há código.

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/VISION.md`](docs/VISION.md) | Visão, problema, objetivos, não objetivos, papéis, roadmap e riscos |
| [`docs/CONSTITUTION.md`](docs/CONSTITUTION.md) | Princípios inegociáveis (P1–P10), arquitetura de referência e implantação por fase |
| [`docs/SPEC.md`](docs/SPEC.md) | Especificação macro: protocolo pareado, matriz de cegamento, épicos, modelo de domínio e MVP |

Em caso de conflito entre documentos, a **Constitution prevalece**.

## Stack prevista

Java (LTS) · Spring Boot · Spring Modulith · PostgreSQL · Flyway · Thymeleaf + HTMX · Testcontainers · Docker Compose

## Próximos passos

1. Plano técnico do MVP a partir da `SPEC.md`
2. Módulo de auditoria (trilha encadeada por hash) e módulo de sorteio (compromisso → revelação)
3. Matriz de cegamento como suíte de testes, antes de qualquer tela

## Autor

Rogerio Cisi

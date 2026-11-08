# Documento de Visão — Crivo

> **Crivo — protocolos cegos para pesquisa de mediunidade.**
> Codinome técnico: **AIR Lab** (de *Anomalous Information Reception*, recepção anômala de informação), usado no código, pacotes e CLI.
> Projeto guarda-chuva: **Dev + Espiritualidade**.
> Autor: Rogerio · Status: rascunho v0.2 · Data: novembro/2026

---

## 1. Contexto

A pesquisa sobre mediunidade costuma esbarrar sempre no mesmo problema: **a credibilidade do método**. Relatos de médiuns que acertam informações sobre pessoas falecidas são numerosos, mas quase nunca são produzidos em condições que excluam as explicações convencionais:
- pistas visuais e verbais;
- leitura fria (*cold reading*);
- pesquisa prévia sobre a pessoa;
- viés de quem avalia;
- fraude;
- seleção posterior dos acertos.

A literatura científica sobre AIR mostra que é possível estudar o fenômeno com rigor. Para isso é preciso usar protocolos com múltiplas camadas de cegamento, alvos sorteados, avaliação independente e análise estatística definida antes da coleta. Na prática, o que torna esses protocolos difíceis de executar é **logística e disciplina**: controlar quem sabe o quê, quando, e provar isso depois.

Esse é um problema de software.

## 2. Declaração de visão

> **Tornar qualquer estudo de recepção anômala de informação verificável por terceiros: cada etapa cega, cada sorteio auditável, cada análise registrada antes do dado existir, e cada resultado (positivo ou nulo) publicado da mesma forma.**

O Crivo não existe para provar que a mediunidade é real nem que não é. Ele existe para que **a resposta, qualquer que seja, seja defensável**.

## 3. Problema que resolve

| Problema hoje | Como o Crivo resolve |
|---|---|
| O cegamento depende da boa vontade e da memória das pessoas | O cegamento é imposto pelo sistema: cada papel só enxerga o que pode enxergar |
| Sorteios feitos à mão ou sem registro | Sorteio por gerador criptográfico, com compromisso (hash) publicado antes |
| Hipóteses e análise ajustadas depois de ver os dados | Pré-registro imutável e com carimbo de tempo antes da primeira sessão |
| Impossível provar a ordem dos eventos | Trilha de auditoria encadeada por hash (*append-only*) |
| Resultados nulos não são publicados | Relatório gerado automaticamente, com o mesmo formato para qualquer resultado |
| Dados presos em planilhas pessoais | Exportação anonimizada e reprodutível (dados abertos) |

## 4. Objetivos

1. **Executar ponta a ponta** um protocolo de leitura mediúnica pareada com cegamento múltiplo (ver SPEC).
2. **Permitir a verificação externa**: um auditor independente consegue conferir, só com os artefatos exportados, que o protocolo foi seguido.
3. **Proteger os participantes**: privacidade (LGPD), consentimento informado e cuidado com pessoas enlutadas.
4. **Ser mantido por um desenvolvedor solo**: arquitetura simples, sem infraestrutura que exija equipe.
5. **Ser extensível a outros protocolos** (por exemplo, resposta livre com alvos de imagem, no estilo ganzfeld) sem reescrever o núcleo.

## 5. Não objetivos

- Não é uma ferramenta de "comunicação com espíritos" nem um oráculo. A plataforma **nunca** apresenta conteúdo como mensagem de um espírito; ela trata **leituras** e **itens** como dados a avaliar.
- Não interpreta resultados do ponto de vista doutrinário.
- Não é rede social, comunidade nem vitrine de médiuns.
- Não é, na v1, um produto multi-tenant comercial.
- Não usa IA para avaliar leituras. A avaliação é humana e cega; a IA pode, no máximo, ajudar na transcrição, de forma local.

## 6. Partes interessadas e papéis

| Papel | Quem é | O que precisa |
|---|---|---|
| **Pesquisador principal** | Rogerio (inicialmente) | Desenhar o estudo, pré-registrar, acompanhar sem quebrar o cegamento |
| **Experimentador (proxy)** | Pessoa que conduz a sessão com o médium no lugar do sitter | Roteiro padronizado; não conhece nada sobre o falecido |
| **Médium** | Participante voluntário | Agenda, instruções claras, devolutiva ao final |
| **Sitter** | Pessoa que conheceu o falecido | Consentimento, privacidade, avaliar as leituras sem saber qual é a dela |
| **Avaliador independente** | Opcional, para protocolos com juízes externos | Formulário cego |
| **Auditor externo** | Qualquer pesquisador cético ou interessado | Artefatos exportados e verificáveis |
| **Público do canal** | Audiência do canal de Espiritismo | Comunicação honesta dos resultados |

## 7. Critérios de sucesso

- **Piloto concluído**: um estudo piloto com pelo menos 1 médium e alguns pares de leituras, executado inteiramente na plataforma, sem nenhuma quebra de cegamento registrada.
- **Auditoria reproduzível**: um terceiro reexecuta a análise a partir da exportação e chega ao mesmo número.
- **Pré-registro verificável**: o hash publicado antes do estudo bate com o plano de análise e com as alocações reveladas no fim.
- **Resultado comunicável**: relatório final gerado e publicável, qualquer que seja o resultado.

## 8. Roadmap macro

| Fase | Entrega | Observação |
|---|---|---|
| **0. Fundamentos** | Repositório, constitution, modelo de domínio, trilha de auditoria, autenticação | Sem participantes reais |
| **1. MVP — protocolo pareado** | Cadastro do estudo, pré-registro, sorteio, sessões, redação, avaliação cega, desmascaramento, análise | Dry-run com dados fictícios |
| **2. Piloto** | Estudo pequeno com participantes reais e voluntários | Avaliar ética e CEP antes, se o objetivo for publicação acadêmica |
| **3. Abertura** | Exportação de dados abertos, página pública de verificação, relatório | Conteúdo para o canal |
| **4. Extensões** | Protocolo de resposta livre (4 alvos), juízes externos, multiestudo | Só depois do piloto |

## 9. Riscos principais

| Risco | Mitigação |
|---|---|
| Vazamento de informação que quebre o cegamento (por exemplo, nomes em e-mails ou metadados de arquivos) | Separação de dados por papel, redação obrigatória, testes automatizados de cegamento |
| Dado sensível de enlutados (causa da morte, crenças religiosas) | LGPD desde o desenho: minimização, pseudonimização, criptografia, consentimento específico |
| Impacto emocional nos sitters | Termo de consentimento claro, devolutiva cuidadosa, possibilidade de desistir a qualquer momento |
| Viés do próprio pesquisador | Pré-registro bloqueado; análise exploratória marcada como tal |
| Escopo crescer demais para um dev solo | MVP restrito a um único protocolo; extensões só depois do piloto |
| Credibilidade questionada por ser "do canal espírita" | Transparência total: código aberto, dados abertos, convite explícito a auditores céticos |

## 10. Documentos relacionados

- `CONSTITUTION.md`: princípios inegociáveis e arquitetura tecnológica.
- `SPEC.md`: especificação macro (escopo, épicos, requisitos, modelo de domínio).
- Documentação de pesquisa existente no Google Drive (pastas *Psicobiofísica* e *TransferênciaAnômalaInformação*), a reconciliar com esta visão.

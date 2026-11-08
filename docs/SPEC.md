# Especificação Macro — Crivo (codinome AIR Lab)

> Spec de alto nível. Cada épico abaixo vira uma spec detalhada própria antes da implementação.
> Conformidade obrigatória com `CONSTITUTION.md`. Contexto e motivação em `VISION.md`.
>
> Versão 0.1 · novembro/2026

---

## 1. Escopo da v1

A v1 implementa **um único protocolo**: a **leitura mediúnica pareada com cegamento múltiplo**. O desenho é inspirado nos protocolos usados na literatura de AIR: médium sem contato com o sitter, experimentador intermediário que não sabe nada sobre o falecido, avaliação feita pelo próprio sitter sem saber qual leitura é a dele.

**Dentro do escopo**: um estudo por vez, um pesquisador, poucos médiuns, dezenas de sitters, exportação de dados e verificação.
**Fora do escopo da v1**: multi-tenant, protocolos de resposta livre (ganzfeld), juízes externos, aplicativo mobile, integração com IA na avaliação.

## 2. Glossário

| Termo | Definição |
|---|---|
| **Estudo** | Uma instância de protocolo, com pré-registro, participantes e análise |
| **Médium** | Participante que produz as leituras |
| **Sitter** | Participante que conheceu uma pessoa falecida e avalia as leituras |
| **Falecido-alvo** | A pessoa falecida indicada por um sitter; o médium recebe apenas o primeiro nome |
| **Par** | Dois falecidos-alvo de perfil semelhante (por exemplo, mesmo gênero e faixa etária). Cada um serve de chamariz para o outro |
| **Experimentador (proxy)** | Quem conduz a sessão com o médium; conhece apenas o primeiro nome do falecido |
| **Leitura** | Conteúdo produzido pelo médium numa sessão (áudio e transcrição) |
| **Leitura redigida** | A leitura convertida em lista numerada de itens, sem pistas não relacionadas ao conteúdo (datas de sessão, referências ao experimentador etc.) |
| **Chamariz** | A leitura do outro falecido do par, apresentada ao sitter como alternativa |
| **Desmascaramento** | Revelação controlada de qual leitura era de qual alvo, depois que todas as avaliações forem fechadas |
| **Pacote de verificação** | Conjunto exportado que permite a um terceiro auditar e reexecutar a análise |

## 3. Fluxo do protocolo (visão ponta a ponta)

```
[1] Pesquisador cria o estudo e escreve o pré-registro (hipóteses + plano de análise)
        │
[2] Sitters dão consentimento e cadastram um falecido-alvo (dados ficam no esquema identity)
        │
[3] Sistema forma os pares por critérios pré-registrados
        │
[4] Sistema sorteia: ordem das leituras, médium por par, experimentador, ordem de apresentação
    → publica o hash (semente + alocações) + carimbo de tempo   ◄── CONGELAMENTO DO PRÉ-REGISTRO
        │
[5] Sessões: o experimentador recebe só o primeiro nome do alvo; o médium lê os dois alvos do par
        │
[6] Transcrição (opcional, local) → redação em lista de itens, feita por alguém que não sabe a qual sitter cada leitura pertence
        │
[7] Cada sitter recebe as duas leituras do par em ordem sorteada, sem rótulo, e:
      • pontua cada item
      • dá uma nota global para cada leitura
      • escolhe qual das duas acredita ser a sua
        │
[8] Todas as avaliações são fechadas e bloqueadas
        │
[9] Desmascaramento (irreversível, auditado) → revelação da semente
        │
[10] Análise pré-registrada + exploratória (rotulada) → relatório → pacote de verificação
```

## 4. Camadas de cegamento (matriz de visibilidade)

Esta matriz é a **especificação de segurança central**. Cada célula "✗" vira um teste automatizado (Constitution P1).

| Informação ↓ / Papel → | Pesquisador | Experimentador | Médium | Redator | Sitter |
|---|---|---|---|---|---|
| Identidade do sitter | ✓ | ✗ | ✗ | ✗ | só a própria |
| Dados do falecido além do primeiro nome | ✓ (até o congelamento) | ✗ | ✗ | ✗ | só o próprio |
| Primeiro nome do falecido | ✓ | ✓ | ✓ (na sessão) | ✗ | só o próprio |
| Ligação leitura → falecido/sitter | ✗ até o desmascaramento | ✓ (só da própria sessão) | ✓ (só da própria sessão) | ✗ | ✗ até o desmascaramento |
| Leitura bruta (áudio) | ✗ | ✓ (própria) | ✗ | ✓ | ✗ |
| Leitura redigida | ✗ até o desmascaramento | ✗ | ✗ | ✓ | ✓ (o par dela, sem rótulo) |
| Avaliações de outros sitters | ✗ até o desmascaramento | ✗ | ✗ | ✗ | ✗ |
| Semente do sorteio | ✗ até a revelação | ✗ | ✗ | ✗ | ✗ |

> Nota: experimentador e redator devem ser **pessoas diferentes**. Se faltar gente no piloto, o desvio é registrado como limitação no pré-registro.

## 5. Épicos

### E1. Estudo e pré-registro
**Objetivo**: criar um estudo e congelar o pré-registro de forma verificável.
- Criar o estudo com título, protocolo (v1: pareado), hipóteses, critérios de pareamento, escalas de pontuação, análise primária e secundária, regras de exclusão e tamanho-alvo da amostra.
- Gerar o documento canônico do pré-registro (Markdown/JSON normalizado) → SHA-256 → carimbo de tempo (RFC 3161/OpenTimestamps) → espelho opcional no OSF.
- Emendas versionadas, com justificativa; as versões anteriores continuam visíveis.

**Aceite**: depois do congelamento, nenhum campo do pré-registro pode ser alterado; a verificação do hash pela CLI confere com o documento publicado.

### E2. Participantes e consentimento
**Objetivo**: cadastrar participantes com privacidade e ética.
- Termo de consentimento versionado, com aceite específico e destacado para dados sensíveis (LGPD) e opção explícita de desistência.
- O sitter cadastra um falecido-alvo: primeiro nome, gênero, faixa etária no óbito e período do falecimento. Detalhes adicionais são opcionais e ficam só para a devolutiva.
- Código pseudônimo gerado para cada participante; os dados identificáveis ficam apenas no esquema `identity`, com criptografia.
- Desistência: os dados do participante são removidos ou anonimizados conforme o termo, e o evento é registrado na trilha.

**Aceite**: nenhuma consulta fora do módulo `participant` acessa o esquema `identity` (teste de arquitetura).

### E3. Pareamento e sorteio
**Objetivo**: formar os pares e sortear as alocações de forma verificável.
- Pareamento por critérios pré-registrados (por exemplo, mesmo gênero e faixa etária).
- Sorteio com `SecureRandom`: médium por par, experimentador por sessão, ordem das leituras e ordem de apresentação ao sitter.
- Esquema compromisso → revelação (Constitution P5).

**Aceite**: com a semente revelada, uma CLI reproduz exatamente as mesmas alocações.

### E4. Sessões
**Objetivo**: conduzir as leituras sem vazamento.
- Agenda de sessões; o experimentador vê apenas o primeiro nome do alvo e um roteiro padronizado.
- Upload do áudio com renomeação para ID aleatório e remoção de metadados.
- Registro de ocorrências (por exemplo, "o médium perguntou algo", "a ligação caiu").
- Transcrição local opcional (whisper.cpp), com revisão humana.

**Aceite**: o arquivo armazenado não contém nome original, metadados de dispositivo nem dados de geolocalização.

### E5. Redação
**Objetivo**: transformar a leitura em lista de itens avaliáveis, sem pistas espúrias.
- O redator vê a transcrição sem saber a qual sitter ela pertence.
- Divide em itens numerados e remove referências ao experimentador, datas da sessão e o primeiro nome do alvo. Essa última remoção é necessária porque, dentro de um par, o nome revelaria a leitura.
- Cada redação fica versionada e auditada.

**Aceite**: as duas leituras de um par chegam ao sitter em formato idêntico e sem o primeiro nome.

### E6. Avaliação cega
**Objetivo**: o sitter avalia as duas leituras do par sem saber qual é a dele.
- Acesso por link de uso único e expirável (sem conta permanente).
- Para cada item: escala pré-registrada (por exemplo, *correto / parcialmente / incorreto / não sei avaliar*).
- Nota global de 0 a 6 para cada leitura e escolha forçada entre as duas, com grau de confiança.
- Ao enviar, a avaliação é bloqueada.

**Aceite**: a interface e a API do sitter não expõem nenhum identificador que permita inferir qual leitura é a dele, nem por ordem, ID sequencial ou data.

### E7. Desmascaramento e análise
**Objetivo**: revelar e analisar exatamente como foi pré-registrado.
- Desmascaramento permitido só quando todas as avaliações estiverem fechadas (ou o prazo pré-registrado tiver expirado). É uma ação explícita e irreversível.
- Revelação da semente.
- Análise primária (sugestão a confirmar no pré-registro): proporção de escolhas corretas comparada a 0,5, com teste binomial unilateral.
- Análises secundárias: nota global alvo × chamariz (Wilcoxon pareado) e proporção de itens corretos alvo × chamariz.
- Análises não pré-registradas aparecem numa seção "Exploratória".
- Relatório final com o mesmo modelo para qualquer resultado.

**Aceite**: a mesma análise, rodada pelo script em R do pacote de verificação, produz os mesmos números (dentro da tolerância numérica).

### E8. Auditoria e verificação
**Objetivo**: permitir que qualquer terceiro confira o estudo.
- Trilha de eventos append-only encadeada por hash.
- CLI `airlab verify` que valida: a cadeia de hashes, o hash do pré-registro, a reprodução do sorteio a partir da semente e a reexecução da análise.
- Registro de incidentes de cegamento, com as sessões afetadas marcadas.

**Aceite**: a alteração manual de qualquer registro no banco é detectada pela CLI.

### E9. Exportação e dados abertos
**Objetivo**: publicar o pacote de verificação.
- O pacote contém: pré-registro, comprovantes de carimbo de tempo, semente, alocações, leituras redigidas, avaliações anonimizadas, trilha de auditoria e script de análise.
- Os dados identificáveis **nunca** entram no pacote; uma checagem automática bloqueia a exportação se detectar um campo do esquema `identity`.

## 6. Modelo de domínio (macro)

```
Study 1──* PreregistrationVersion (hash, timestamp proof, frozen_at)
Study 1──* Pair 1──2 Target ──1 Sitter(code)
Study 1──1 AllocationSet (seed_commitment, seed_revealed_at)
Pair 1──2 Session ──1 Medium(code), Experimenter(code)
Session 1──1 Reading 1──* RedactedReading(version) 1──* Item
Sitter 1──1 ScoringPacket ──2 RedactedReading (ordem sorteada)
ScoringPacket 1──* ItemScore, 2 GlobalScore, 1 ForcedChoice
Study 1──0..1 Unblinding ──1 AnalysisRun 1──1 Report
Participant(identity schema) 1──* Consent(version)
AuditEvent (seq, prev_hash, hash, type, payload, at)
Incident ──* Session (affected)
```

## 7. Requisitos não funcionais

| Categoria | Requisito |
|---|---|
| Segurança | 2FA para pesquisadores; links de uso único para sitters; HTTPS obrigatório; criptografia em repouso para o esquema identity e para mídia |
| Privacidade | Logs sem dados pessoais; minimização; base legal e consentimento documentados (LGPD) |
| Integridade | Tabela de auditoria sem permissão de UPDATE/DELETE para o usuário da aplicação |
| Reprodutibilidade | Análise determinística; versões de bibliotecas fixadas no pacote |
| Disponibilidade | Não crítica; backup diário criptografado e restauração testada mensalmente |
| Usabilidade | Telas de sitter simples, acessíveis e usáveis no celular; linguagem acolhedora |
| Manutenibilidade | Monólito modular; fronteiras verificadas em teste; documentação viva em `/docs` |
| Desempenho | Volume pequeno (dezenas a centenas de registros); sem requisito especial |

## 8. Corte do MVP

**Entra**: E1, E2 (com termo simples), E3, E4 (sem transcrição automática), E5, E6, E7 (somente a análise primária), E8 (cadeia de hashes e CLI de verificação básica).
**Fica para depois**: transcrição local, espelho no OSF, análise secundária completa, E9 completo, painel de acompanhamento.

**Marco de validação do MVP**: um *dry-run* completo com participantes fictícios (pessoas reais simulando os papéis com falecidos inventados). O objetivo é exercitar todo o fluxo e tentar quebrar o cegamento de propósito antes do piloto real.

## 9. Questões em aberto

1. **Tamanho da amostra**: precisa de cálculo de poder formal. Como referência, para detectar 65% de acerto contra 50% (α = 0,05 unilateral, poder de 80%) são necessárias cerca de 70 escolhas independentes. Também é preciso decidir como tratar a dependência entre os dois sitters de um mesmo par.
2. **Escala de itens**: qual escala usar e como tratar o "não sei avaliar".
3. **Primeiro nome**: dar ao médium o nome real ou um pseudônimo? (Isso afeta a ética e a validade.)
4. **Recrutamento de médiuns**: critérios de inclusão e se haverá triagem prévia.
5. **CEP**: o estudo terá fins de publicação acadêmica? Se sim, a submissão à Plataforma Brasil vem antes do piloto.
6. **Devolutiva**: o que cada participante recebe ao final, e como comunicar resultados nulos com cuidado.
7. **Reconciliação** com a documentação de pesquisa já existente no Google Drive (*TransferênciaAnômalaInformação*).

## 10. Próximos passos sugeridos (no Claude Code)

1. Criar o repositório e colocar `VISION.md`, `CONSTITUTION.md` e `SPEC.md` em `/docs`; referenciar os três no `CLAUDE.md`.
2. Gerar o **plano técnico** do MVP a partir desta spec (estrutura de módulos, migrações iniciais, esquemas e papéis do banco).
3. Implementar primeiro **E8 (auditoria)** e **E3 (sorteio)**: são a base de confiança de todo o resto.
4. Depois, a matriz de cegamento (seção 4) como suíte de testes, ainda antes das telas.

# Especificação Macro — Crivo (codinome AIR Lab)

> Spec de alto nível. Cada épico abaixo vira uma spec detalhada própria antes da implementação.
> Conformidade obrigatória com `CONSTITUTION.md`. Contexto e motivação em `VISION.md`. Fundamentação científica em `REFERENCES.md`.
>
> Versão 0.5 · novembro/2026 · 0.2: seção 2.1 "O que é uma leitura", regra "um item = uma afirmação" e referências · 0.3: critérios de inclusão de médiuns no pré-registro, regra de parada vinculante e unidade de análise por par (seções 5 e 9) · 0.4: medida primária pareada com Wilcoxon exato, teste de permutação de Pratt e Birge, controles de comprimento e de pistas temporais, escala de itens de cinco categorias e desvios de protocolo — derivados dos artigos em `references/` · 0.5: *sitter* passa a ser **consulente** em todo o texto em português, com a forma inglesa registrada no glossário e mantida nos identificadores do código

---

## 1. Escopo da v1

A v1 implementa **um único protocolo**: a **leitura mediúnica pareada com cegamento múltiplo**. O desenho é inspirado nos protocolos usados na literatura de AIR: médium sem contato com o consulente, experimentador intermediário que não sabe nada sobre o falecido, avaliação feita pelo próprio consulente sem saber qual leitura é a dele.

**Dentro do escopo**: um estudo por vez, um pesquisador, poucos médiuns, dezenas de consulentes, exportação de dados e verificação.
**Fora do escopo da v1**: multi-tenant, protocolos de resposta livre (ganzfeld), juízes externos, aplicativo mobile, integração com IA na avaliação.

## 2. Glossário

| Termo | Definição |
|---|---|
| **Estudo** | Uma instância de protocolo, com pré-registro, participantes e análise |
| **Médium** | Participante que produz as leituras |
| **Consulente** (em inglês, *sitter*) | Participante que conheceu uma pessoa falecida e avalia as leituras. A literatura internacional usa *sitter*: é o termo a procurar nos artigos e o identificador que permanece no código (`sitter_code`, `ParticipantRole.SITTER`) |
| **Falecido-alvo** | A pessoa falecida indicada por um consulente; o médium recebe apenas o primeiro nome |
| **Par** | Dois falecidos-alvo de perfil semelhante (por exemplo, mesmo gênero e faixa etária). Cada um serve de chamariz para o outro |
| **Experimentador (proxy)** | Quem conduz a sessão com o médium; conhece apenas o primeiro nome do falecido |
| **Leitura** | Conteúdo produzido pelo médium numa sessão (áudio e transcrição) |
| **Leitura redigida** | A leitura convertida em lista numerada de itens, sem pistas não relacionadas ao conteúdo (datas de sessão, referências ao experimentador etc.) |
| **Chamariz** | A leitura do outro falecido do par, apresentada ao consulente como alternativa |
| **Desmascaramento** | Revelação controlada de qual leitura era de qual alvo, depois que todas as avaliações forem fechadas |
| **Pacote de verificação** | Conjunto exportado que permite a um terceiro auditar e reexecutar a análise |

### 2.1 O que é uma leitura

**Leitura** é tudo o que o médium produz numa sessão a respeito de um falecido-alvo: as afirmações que ele faz sobre aquela pessoa. É a matéria-prima do estudo, o dado bruto a ser avaliado.

O termo vem do inglês *reading*, usado na pesquisa sobre mediunidade. No vocabulário espírita, o mais próximo seria "comunicação" ou "mensagem" (por psicofonia ou psicografia). A troca é deliberada: "comunicação" já pressupõe que alguém está se comunicando, enquanto "leitura" descreve só o que aconteceu (o médium falou e isso foi registrado) sem afirmar de onde veio a informação. É o princípio P3 da Constitution, de neutralidade.

**Ciclo de vida de uma leitura no protocolo:**

1. **Sessão:** o experimentador diz ao médium apenas o primeiro nome do alvo (por exemplo, "Antônio"). O médium fala livremente sobre essa pessoa (aparência, personalidade, circunstâncias da morte, hobbies, relações, recados), e tudo é gravado.
2. **Leitura bruta:** a gravação e a sua transcrição.
3. **Leitura redigida:** a transcrição é convertida numa lista numerada de **itens**, cada um com **uma única afirmação verificável**. Saem as pistas espúrias, como o primeiro nome, referências ao experimentador e datas. Exemplo inventado:
   1. Era um homem alto, de mãos grandes.
   2. Gostava de mexer com marcenaria.
   3. Faleceu de forma repentina, ligada ao coração.
   4. Tinha uma relação difícil com um irmão.
   5. Mostra um relógio de bolso como algo importante.
4. **Avaliação cega:** o consulente recebe duas leituras redigidas, a do seu falecido e a do chamariz, sem saber qual é qual. Ele pontua cada item, dá uma nota global para cada leitura e escolhe qual acredita ser a sua.

**Por que itens e chamariz:** afirmações genéricas ("era uma pessoa querida pela família") tendem a "acertar" para qualquer um. O chamariz expõe exatamente isso: só conta como evidência o que se ajusta ao alvo verdadeiro **mais** do que ao chamariz, de forma consistente e acima do acaso. A decomposição em itens verificáveis segue a prática da literatura (ver `REFERENCES.md`, Rocha et al. 2014 e Beischel et al. 2015).

## 3. Fluxo do protocolo (visão ponta a ponta)

```
[1] Pesquisador cria o estudo e escreve o pré-registro (hipóteses + plano de análise)
        │
[2] Consulentes dão consentimento e cadastram um falecido-alvo (dados ficam no esquema identity)
        │
[3] Sistema forma os pares por critérios pré-registrados
        │
[4] Sistema sorteia: ordem das leituras, médium por par, experimentador, ordem de apresentação
    → publica o hash (semente + alocações) + carimbo de tempo   ◄── CONGELAMENTO DO PRÉ-REGISTRO
        │
[5] Sessões: o experimentador recebe só o primeiro nome do alvo; o médium lê os dois alvos do par
        │
[6] Transcrição (opcional, local) → redação em lista de itens, feita por alguém que não sabe a qual consulente cada leitura pertence
        │
[7] Cada consulente recebe as duas leituras do par em ordem sorteada, sem rótulo, e:
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

| Informação ↓ / Papel → | Pesquisador | Experimentador | Médium | Redator | Consulente |
|---|---|---|---|---|---|
| Identidade do consulente | ✓ | ✗ | ✗ | ✗ | só a própria |
| Dados do falecido além do primeiro nome | ✓ (até o congelamento) | ✗ | ✗ | ✗ | só o próprio |
| Primeiro nome do falecido | ✓ | ✓ | ✓ (na sessão) | ✗ | só o próprio |
| Ligação leitura → falecido/consulente | ✗ até o desmascaramento | ✓ (só da própria sessão) | ✓ (só da própria sessão) | ✗ | ✗ até o desmascaramento |
| Leitura bruta (áudio) | ✗ | ✓ (própria) | ✗ | ✓ | ✗ |
| Leitura redigida | ✗ até o desmascaramento | ✗ | ✗ | ✓ | ✓ (o par dela, sem rótulo) |
| Avaliações de outros consulentes | ✗ até o desmascaramento | ✗ | ✗ | ✗ | ✗ |
| Semente do sorteio | ✗ até a revelação | ✗ | ✗ | ✗ | ✗ |

> Nota: experimentador e redator devem ser **pessoas diferentes**. Se faltar gente no piloto, o desvio é registrado como limitação no pré-registro.

## 5. Épicos

### E1. Estudo e pré-registro
**Objetivo**: criar um estudo e congelar o pré-registro de forma verificável.
- Criar o estudo com título, protocolo (v1: pareado), hipóteses, **critérios de recrutamento e triagem de médiuns**, critérios de pareamento, escalas de pontuação, análise primária e secundária, regras de exclusão, **medida primária**, **efeito esperado**, **unidade de análise**, **regra de parada**, tamanho-alvo da amostra e **diferença máxima de itens entre as duas leituras de um par**.
- O documento congelado declara o **poder estatístico** do desenho: o mínimo de unidades para o efeito declarado e se o tamanho-alvo o atende. Um desenho subdimensionado não é recusado — um piloto é legítimo — mas fica dito no próprio pré-registro, para que um resultado nulo não possa depois ser apresentado como ausência de efeito.
- **Desvios de protocolo** são registrados contra a versão congelada, com data e motivo, em tabela *append-only*, e aparecem no relatório ao lado do resultado. Desvio não é emenda: a emenda muda o plano e cria nova versão, o desvio registra que a execução se afastou do plano que já estava congelado (Nosek et al., 2018).
- Os critérios de inclusão de médiuns são pré-registrados porque selecionar médiuns por desempenho anterior infla o efeito. Os resultados positivos da literatura vêm de "médiuns selecionados" sem que os critérios de seleção tenham sido publicados; aqui eles entram no documento com hash, antes da coleta.
- Gerar o documento canônico do pré-registro (Markdown/JSON normalizado) → SHA-256 → carimbo de tempo (RFC 3161/OpenTimestamps) → espelho opcional no OSF.
- Emendas versionadas, com justificativa; as versões anteriores continuam visíveis.

**Aceite**: depois do congelamento, nenhum campo do pré-registro pode ser alterado; a verificação do hash pela CLI confere com o documento publicado.

### E2. Participantes e consentimento
**Objetivo**: cadastrar participantes com privacidade e ética.
- Termo de consentimento versionado, com aceite específico e destacado para dados sensíveis (LGPD) e opção explícita de desistência.
- O consulente cadastra um falecido-alvo: primeiro nome, gênero, faixa etária no óbito e período do falecimento. Detalhes adicionais são opcionais e ficam só para a devolutiva.
- Código pseudônimo gerado para cada participante; os dados identificáveis ficam apenas no esquema `identity`, com criptografia.
- Desistência: os dados do participante são removidos ou anonimizados conforme o termo, e o evento é registrado na trilha.

**Aceite**: nenhuma consulta fora do módulo `participant` acessa o esquema `identity` (teste de arquitetura).

### E3. Pareamento e sorteio
**Objetivo**: formar os pares e sortear as alocações de forma verificável.
- Pareamento por critérios pré-registrados (por exemplo, mesmo gênero e faixa etária).
- Sorteio com `SecureRandom`: médium por par, experimentador por sessão, ordem das leituras, ordem de apresentação ao consulente e **qual dos dois consulentes do par é a unidade independente da análise confirmatória** (ver E7).
- Esquema compromisso → revelação (Constitution P5).

**Aceite**: com a semente revelada, uma CLI reproduz exatamente as mesmas alocações.

### E4. Sessões
**Objetivo**: conduzir as leituras sem vazamento.
- Agenda de sessões **contrabalanceada**: o sorteio atribui índices de dia e de horário, de modo que nenhum par ocupe sistematicamente o primeiro ou o último horário do dia. É a defesa contra pistas temporais — uma referência ao tempo dentro da leitura ("aquele acidente de hoje") permite ao consulente deduzir qual leitura é a dele. O'Keeffe e Wiseman (2005) foram os primeiros a controlar isso.
- O experimentador vê apenas o primeiro nome do alvo e um roteiro padronizado.
- Upload do áudio com renomeação para ID aleatório e remoção de metadados.
- Registro de ocorrências (por exemplo, "o médium perguntou algo", "a ligação caiu").
- Transcrição local opcional (whisper.cpp), com revisão humana.

**Aceite**: o arquivo armazenado não contém nome original, metadados de dispositivo nem dados de geolocalização.

### E5. Redação
**Objetivo**: transformar a leitura em lista de itens avaliáveis, sem pistas espúrias.
- O redator vê a transcrição sem saber a qual consulente ela pertence.
- Divide em itens numerados, com **uma única afirmação verificável por item**. Itens com várias afirmações dificultam a pontuação, uma crítica feita ao formato de O'Keeffe & Wiseman, 2005 (ver `REFERENCES.md`).
- Remove referências ao experimentador, datas da sessão e o primeiro nome do alvo. Essa última remoção é necessária porque, dentro de um par, o nome revelaria a leitura.
- Cada redação fica versionada e auditada.

**Aceite**: as duas leituras de um par chegam ao consulente em formato idêntico e sem o primeiro nome.

### E6. Avaliação cega
**Objetivo**: o consulente avalia as duas leituras do par sem saber qual é a dele.
- Acesso por link de uso único e expirável (sem conta permanente).
- Para cada item: escala pré-registrada de cinco categorias — *encaixe óbvio / encaixe parcial ou interpretado / sem encaixe / **encaixa em outra pessoa próxima** / não sei avaliar*. As duas categorias intermediárias exigem que o avaliador **escreva a interpretação**, e só o encaixe óbvio conta para a acurácia da medida primária. A categoria "encaixa em outra pessoa" existe porque, sem ela, o item que descreve um parente vivo é forçado a virar acerto ou erro, e em qualquer dos dois casos a explicação alternativa desaparece da conta.
- Os itens são apresentados em **ordem sorteada**, derivada da semente, para que uma pista num item não contamine a leitura da lista inteira.
- As duas leituras de um par chegam com **comprimento comparável**: se a diferença de itens passar da tolerância pré-registrada, a mais longa é cortada por subamostra sorteada da semente. Nos dados de O'Keeffe e Wiseman as leituras iam de 6 a 92 afirmações, e a mais longa e mais genérica recebeu as notas mais altas de todos os consulentes — comprimento, por si só, é pista e é confundidor.
- Nota global de 0 a 6 para cada leitura e escolha forçada entre as duas, com grau de confiança.
- Ao enviar, a avaliação é bloqueada.

**Aceite**: a interface e a API do consulente não expõem nenhum identificador que permita inferir qual leitura é a dele, nem por ordem, ID sequencial ou data.

### E7. Desmascaramento e análise
**Objetivo**: revelar e analisar exatamente como foi pré-registrado.
- Desmascaramento permitido só quando todas as avaliações estiverem fechadas (ou o prazo pré-registrado tiver expirado). É uma ação explícita e irreversível.
- Revelação da semente.
- **Análise primária**: diferença pareada alvo − chamariz, uma observação por par, com **teste de Wilcoxon exato unilateral**. A diferença é medida na nota global ou no percentual de itens com encaixe óbvio, conforme declarado no pré-registro.
- **Análises secundárias**: proporção de escolhas forçadas corretas comparada a 0,5 (binomial exato), e **teste de permutação de Pratt e Birge** sobre a matriz de notas, que é a análise mais endossada no campo por não assumir independência entre os itens de uma leitura.
- A escolha forçada **deixa de ser a medida primária**. Ela descarta o tamanho da diferença, e Beischel e Schwartz já esperavam que fosse o indicador menos sensível — os números deles confirmaram (notas globais p = 0,007; escolha forçada p = 0,01). Com o efeito que a meta-análise de Sarraf et al. (2021) reporta, a troca decide a viabilidade do estudo: ver a tabela em `docs/PLAN.md`, seção 4.
- **Análise vedada**: multiplicar as probabilidades individuais dos itens como se fossem independentes (Hyslop, 1919). Infla a acurácia, porque os itens de uma leitura são correlacionados por construção, e já foi refutada na própria literatura do campo.
- **Unidade de análise: o par.** Os dois consulentes de um par avaliam o mesmo material, em espelho, e por isso as duas escolhas não são independentes. Entra na análise confirmatória apenas a escolha do consulente que o sorteio designou para aquele par, definida antes da primeira sessão. A escolha do outro consulente é coletada e relatada como análise secundária.
- **Regra de parada vinculante.** A coleta para no tamanho-alvo pré-registrado, sem análise interina. Se os dados não corresponderem ao que foi declarado — coleta interrompida antes, continuada depois, ou pré-registro não congelado —, o sistema **rotula o resultado como exploratório** e imprime o motivo ao lado. Isso responde à crítica de parada opcional (*optional stopping*).
- Análises secundárias: nota global alvo × chamariz (Wilcoxon pareado) e proporção de itens corretos alvo × chamariz.
- Análises não pré-registradas aparecem numa seção "Exploratória".
- Relatório final com o mesmo modelo para qualquer resultado.

**Aceite**: a mesma análise, rodada pelo script em R do pacote de verificação, produz os mesmos números (dentro da tolerância numérica). Uma análise fora dos termos pré-registrados nunca aparece rotulada como confirmatória, nem no relatório nem no pacote exportado.

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
Consulente 1──1 ScoringPacket ──2 RedactedReading (ordem sorteada)
ScoringPacket 1──* ItemScore, 2 GlobalScore, 1 ForcedChoice
Study 1──0..1 Unblinding ──1 AnalysisRun 1──1 Report
Participant(identity schema) 1──* Consent(version)
AuditEvent (seq, prev_hash, hash, type, payload, at)
Incident ──* Session (affected)
```

## 7. Requisitos não funcionais

| Categoria | Requisito |
|---|---|
| Segurança | 2FA para pesquisadores; links de uso único para consulentes; HTTPS obrigatório; criptografia em repouso para o esquema identity e para mídia |
| Privacidade | Logs sem dados pessoais; minimização; base legal e consentimento documentados (LGPD) |
| Integridade | Tabela de auditoria sem permissão de UPDATE/DELETE para o usuário da aplicação |
| Reprodutibilidade | Análise determinística; versões de bibliotecas fixadas no pacote |
| Disponibilidade | Não crítica; backup diário criptografado e restauração testada mensalmente |
| Usabilidade | Telas de consulente simples, acessíveis e usáveis no celular; linguagem acolhedora |
| Manutenibilidade | Monólito modular; fronteiras verificadas em teste; documentação viva em `/docs` |
| Desempenho | Volume pequeno (dezenas a centenas de registros); sem requisito especial |

## 8. Corte do MVP

**Entra**: E1, E2 (com termo simples), E3, E4 (sem transcrição automática), E5, E6, E7 (somente a análise primária), E8 (cadeia de hashes e CLI de verificação básica).
**Fica para depois**: transcrição local, espelho no OSF, análise secundária completa, E9 completo, painel de acompanhamento.

**Marco de validação do MVP**: um *dry-run* completo com participantes fictícios (pessoas reais simulando os papéis com falecidos inventados). O objetivo é exercitar todo o fluxo e tentar quebrar o cegamento de propósito antes do piloto real.

## 9. Questões em aberto

1. **Tamanho da amostra**: precisa de cálculo de poder formal, e agora com uma consequência prática em cima da mesa. A dependência entre os dois consulentes de um par **está decidida** (unidade de análise = par, ver E7), e isso tem custo: as cerca de 70 escolhas independentes necessárias para detectar 65% contra 50% (α = 0,05 unilateral, poder de 80%) passam a significar **70 pares, ou seja 140 consulentes e 140 sessões**, não 70 consulentes. A alternativa é usar as duas escolhas de cada par com um método que corrija o agrupamento (por exemplo GEE ou erro-padrão robusto por cluster), o que recupera parte do poder mas exige uma decisão estatística e uma implementação que a v1 não tem. **Decidido na v0.4**: a medida primária passa a ser a diferença pareada contínua, que mantém o par como unidade e recupera o poder — cerca de 30 pares para d = 0,5, contra 240 pares para 58% de acerto na escolha forçada. O cálculo está implementado (`PowerCheck`) e entra no documento congelado. **O que resta decidir**: qual efeito declarar. Os efeitos por seção de leitura em Beischel et al. (2015) vão de d = 0,49 a 0,75, e a seção escolhida muda o tamanho do estudo.
2. **Escala de itens**: qual escala usar e como tratar o "não sei avaliar".
3. **Primeiro nome**: dar ao médium o nome real ou um pseudônimo? (Isso afeta a ética e a validade.)
4. **Recrutamento de médiuns**: o campo de critérios já existe no pré-registro e é obrigatório; falta decidir o conteúdo — quais critérios de inclusão e se haverá triagem prévia. Se houver triagem por desempenho, ela precisa estar descrita ali, porque é exatamente o ponto que a crítica usa para explicar resultados positivos.
5. **CEP**: o estudo terá fins de publicação acadêmica? Se sim, a submissão à Plataforma Brasil vem antes do piloto.
6. **Devolutiva**: o que cada participante recebe ao final, e como comunicar resultados nulos com cuidado.
7. **Reconciliação** com a documentação de pesquisa já existente no Google Drive (*TransferênciaAnômalaInformação*).

## 10. Próximos passos sugeridos (no Claude Code)

1. Criar o repositório e colocar `VISION.md`, `CONSTITUTION.md` e `SPEC.md` em `/docs`; referenciar os três no `CLAUDE.md`.
2. Gerar o **plano técnico** do MVP a partir desta spec (estrutura de módulos, migrações iniciais, esquemas e papéis do banco).
3. Implementar primeiro **E8 (auditoria)** e **E3 (sorteio)**: são a base de confiança de todo o resto.
4. Depois, a matriz de cegamento (seção 4) como suíte de testes, ainda antes das telas.

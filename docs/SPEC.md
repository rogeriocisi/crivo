# Especificação Macro — Crivo (codinome AIR Lab)

> Spec de alto nível. Cada épico abaixo vira uma spec detalhada própria antes da implementação.
> Conformidade obrigatória com `CONSTITUTION.md`. Contexto e motivação em `VISION.md`. Fundamentação científica em `REFERENCES.md`.
>
> Versão 0.9 · novembro/2026 · 0.2: seção 2.1 "O que é uma leitura", regra "um item = uma afirmação" e referências · 0.3: critérios de inclusão de médiuns no pré-registro, regra de parada vinculante e unidade de análise por par (seções 5 e 9) · 0.4: medida primária pareada com Wilcoxon exato, teste de permutação de Pratt e Birge, controles de comprimento e de pistas temporais, escala de itens de cinco categorias e desvios de protocolo — derivados dos artigos em `references/` · 0.5: *sitter* passa a ser **consulente** em todo o texto em português, com a forma inglesa registrada no glossário e mantida nos identificadores do código · 0.6: épico E10, autoatendimento e automação, com as travas obrigatórias · 0.7: o Crivo passa a ser uma plataforma de protocolos de cognição anômala, com catálogo de protocolos (seção 1.1) e os épicos E11 e E12 — dois testes integralmente automatizáveis · 0.8: a semente dos testes de autoatendimento passa a ter duas metades, uma do servidor e uma do participante, o que remove a necessidade de confiar na plataforma quanto à ordem dos fatos · 0.9: protocolo **P4, leitura estruturada automatizada** (épico E13), o ramo AIR integralmente automatizado: contas com perfil fixo (consulente ou médium), questionário fechado preenchido pelo consulente e pelo médium e conferência automática contra o alvo e o chamariz; o catálogo passa a se dividir em **protocolos AIR** e **experimentos extras**

---

## 1. Escopo

O Crivo é uma plataforma de **protocolos cegos para pesquisa de cognição anômala**. O primeiro ramo é a **recepção anômala de informação (AIR)**, isto é a mediunidade, e nele está o protocolo que motivou o projeto: a leitura pareada com cegamento múltiplo. O segundo ramo são os testes de **percepção extrassensorial (ESP)**, nos quais não há informação sobre pessoa falecida e o alvo é sorteado pelo próprio sistema.

A distinção entre os dois ramos não é cosmética. Na literatura, *anomalous information reception* designa especificamente a obtenção de informação sobre um falecido sem via sensorial; um teste de precognição com alvo gerado depois da resposta não é AIR, e chamá-lo assim seria impreciso. O termo que cobre os dois é **cognição anômala**, e é o que a plataforma usa. O codinome técnico **AIR Lab** permanece nos pacotes Java, na CLI e nos artefatos Maven, como herança do primeiro protocolo.

O ramo AIR tem dois protocolos. O **P1** é conduzido por uma equipe (experimentador *proxy* e redator) e é o mais próximo da literatura. O **P4** roda inteiro pelo site, sem ninguém da equipe no circuito: o consulente responde a um questionário fechado sobre o falecido, o médium responde ao mesmo questionário conhecendo só o primeiro nome, e o sistema confere as respostas. Os dois convivem porque cada um paga um preço diferente, e cada um declara o seu (seção 1.1, regra 2).

**Dentro do escopo**: um estudo por vez e por protocolo, um pesquisador (o **administrador** da instalação), exportação de dados e verificação; o P4 com inscrição aberta por conta; os dois testes de ESP em autoatendimento público.
**Fora do escopo**: multi-tenant, ganzfeld (exige emissor e cabine), presentimento e DMILS (exigem sensores), ESP em sonho (exige pernoite), juízes externos remunerados, aplicativo mobile, integração com IA na avaliação.

### 1.1 Catálogo de protocolos

O catálogo se apresenta em duas seções: **protocolos AIR**, com o P4 em destaque, e **experimentos extras**, com os testes de ESP.

| Seção | Código | Nome público | Ramo | Acaso | Automação | Épico |
|---|---|---|---|---|---|---|
| Protocolos AIR | **P4** | Leitura estruturada automatizada | AIR (mediunidade) | 50% | **Integral**: conta, pedido, leitura, conferência e avisos sem ninguém da equipe | E13 |
| Protocolos AIR | **P1** | Leitura pareada cega | AIR (mediunidade) | 50% | Parcial: exige equipe — sessão síncrona com experimentador *proxy* e redator | E1–E9 |
| Experimentos extras | **P2** | Escolha forçada precognitiva | ESP (precognição) | 25% | **Integral** | E11 |
| Experimentos extras | **P3** | Visão remota com quatro alvos | ESP (resposta livre) | 25% | **Integral** | E12 |

Os códigos não são renumerados: eles já aparecem em trilhas, sessões e no repositório, e a ordem de apresentação é decisão de página, não de identidade do protocolo.

Regras do catálogo:

1. **Todo protocolo herda as mesmas garantias**: pré-registro congelado, sorteio com compromisso → revelação, trilha de auditoria encadeada por hash, rótulo confirmatório decidido pela política e pacote de verificação. Um protocolo que não possa oferecê-las não entra.
2. **Cada protocolo declara o que o seu resultado não sustenta.** O P1 não distingue entre sobrevivência e telepatia com os vivos; o P2 e o P3 não dizem nada sobre falecidos. Isso é texto obrigatório na página do protocolo, não nota de rodapé.
3. **O acaso é declarado e verificável**, não estimado a partir dos dados.
4. **P2 e P3 não substituem o P1.** Eles existem porque são integralmente automatizáveis e porque qualquer pessoa pode conferir a lisura do próprio teste depois; servem de porta de entrada e de vitrine do método, não de prova sobre mediunidade.
5. **P4 e P1 não se substituem.** O P4 ganha pontuação objetiva — ninguém julga se um item "encaixa" — e automação integral; perde o experimentador *proxy* e a resposta livre. O P1 faz o caminho inverso. Um resultado de um não vale como replicação do outro.

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
| **Administrador** | Quem opera a instalação e conduz o estudo: o pesquisador. Não é perfil que alguém escolha ao se inscrever; a conta vem da configuração do servidor |
| **Pedido de consulta** (P4) | O que o consulente envia: o primeiro nome do falecido e as respostas dele ao questionário, travadas com hash antes de qualquer médium receber o pedido |
| **Questionário** (P4) | Lista versionada de campos fechados sobre o falecido, a mesma para o consulente e para o médium, congelada no pré-registro |
| **Leitura estruturada** (P4) | As respostas do médium ao questionário, conhecendo só o primeiro nome do alvo |

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
- Acesso por link de uso único e expirável (sem conta permanente). Vale para o P1; no P4 o consulente tem conta, mas não avalia leitura nenhuma (E13).
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

### E10. Autoatendimento e automação do estudo

**Objetivo**: permitir que um estudo rode de ponta a ponta pela internet — consulentes se
inscrevem, consentem e cadastram o alvo; médiuns se cadastram e recebem o primeiro nome pelo
sistema; a avaliação acontece no site por link de uso único; a análise roda sozinha ao atingir o
N pré-registrado — **sem que a automação enfraqueça nenhuma garantia do protocolo**.

O critério que decide o que entra neste épico é um só: *automatizar o que fecha uma via de
vazamento ou remove um grau de liberdade do pesquisador; não automatizar o que só reduz esforço
à custa de validade.*

#### O que a automação fortalece (entra)

- **Liberação do primeiro nome sem intermediário.** Quanto menos gente vê o dado, menor a
  superfície de vazamento. A entrega pelo sistema é mais cega do que a entrega por uma pessoa.
- **Link de uso único e expirável** para a avaliação, com o token guardado apenas como hash.
- **Pareamento, sorteio e ordem de apresentação** sem intervenção manual — já implementados.
- **Qualificação cega de médiuns**: rodada prévia de leituras avaliadas pelo mesmo mecanismo,
  antes da entrada no estudo. É a triagem que a literatura associa mais fortemente a resultado,
  e é integralmente automatizável.
- **Fechamento automático** quando o N pré-registrado é atingido, com a análise rodando sob a
  política de rótulo confirmatório já existente.

#### O que a automação enfraquece (fica de fora)

- **A sessão não vira formulário.** Substituir o experimentador *proxy* por um campo de texto
  elimina a variável que mais separa os estudos positivos dos nulos na comparação de Silva e
  Moreira-Almeida (2022): dos dois estudos em que o médium não interagiu com ninguém, ambos
  deram nulo. O agendamento, a liberação do nome, o upload do áudio e a transcrição são
  automatizados; a sessão permanece síncrona, com *proxy*. Um braço assíncrono só é admissível
  como variante **declarada no pré-registro**, nunca como padrão. **O P4 (E13) é essa variante**,
  declarada como protocolo próprio, com a ausência do *proxy* escrita entre os limites dele; o
  P1 continua síncrono.
- **Nenhuma avaliação automática de conteúdo**, em nenhuma etapa (P10).

#### Travas obrigatórias

1. **Nenhum resultado parcial antes do desmascaramento.** O sistema mostra *progresso* —
   quantas unidades independentes faltam para o alvo — e nunca taxa de acerto, diferença entre
   alvo e chamariz, ou qualquer função dos escores. A projeção de progresso não tem campo de
   desfecho, por construção, e há teste estrutural que verifica isso. Sem esta trava, a
   automação reintroduz a parada opcional que o P2 existe para impedir.
2. **Papel único por pessoa em cada estudo.** Ninguém é médium e consulente no mesmo estudo, e
   experimentador e redator são pessoas diferentes. A tentativa é recusada no domínio e
   registrada na trilha. Com cadastro aberto, o conluio é o risco de fraude mais sério, e é o
   único que destrói o resultado sem deixar rastro óbvio.
3. **Identidade verificada antes da inscrição valer.** Confirmação de e-mail no mínimo; para
   estudo com fim de publicação, verificação mais forte definida no pré-registro. Contas
   ligadas (mesmo contato, mesmo alvo cadastrado duas vezes) são sinalizadas para revisão
   humana, não bloqueadas em silêncio.
4. **Comitê de ética antes do ar, não depois.** Recrutamento público de pessoas enlutadas, com
   coleta de dado sensível, exige aprovação do CEP/Plataforma Brasil **antes** de a inscrição
   ser aberta. O sistema registra a referência da aprovação no pré-registro.
5. **Um humano alcançável.** Alguém pode se inscrever em luto agudo. A inscrição exibe um canal
   de contato humano e o estudo registra quem é a pessoa responsável; resposta automática não
   substitui isso.
6. **Consentimento sempre na versão corrente.** Uma inscrição feita sob versão antiga do termo
   não é aceita sem novo aceite, e a versão aceita fica gravada com a inscrição.

**Aceite**: um estudo completo roda sem intervenção manual exceto a sessão e a revisão de
sinalizações; em nenhum momento, antes do desmascaramento, qualquer tela ou API expõe função
dos escores; e a tentativa de acumular papéis conflitantes é recusada e auditada.

### E11. Escolha forçada precognitiva (P2)

**Objetivo**: um teste que qualquer pessoa faz pelo navegador, sem intermediário, e cuja lisura ela pode conferir depois por conta própria.

**Desenho**: a cada tentativa, a pessoa escolhe entre quatro alternativas. O alvo daquela tentativa é derivado de uma semente sorteada no início da sessão e **só é calculado e mostrado depois que a resposta está travada**. Acaso = 25%, declarado.

- A sessão tem número de tentativas **fixo e declarado antes de começar** (padrão: 24). A quantidade não muda no meio.
- **A semente tem duas metades, e nenhum dos dois lados a escolhe sozinho:**

  ```
  semente = SHA-256( metade do servidor || metade do participante )
  ```

  O servidor sorteia a metade dele com `SecureRandom`, guarda cifrada e **publica o hash dela antes de a outra metade existir**. Só então o navegador do participante sorteia a metade dele (16 bytes de `crypto.getRandomValues`) e a envia. Para dirigir o resultado, o servidor precisaria de uma metade que, combinada com a do participante, desse a semente desejada — e ele já estava comprometido antes de ver aquela metade. Isso é um ataque de pré-imagem ao SHA-256.

- **Antes da contribuição do participante a sessão não tem semente nem tentativas.** A regra é imposta no banco, não só no código: um *trigger* recusa qualquer linha de tentativa de sessão sem a metade do participante (migração V16). É essa ordem que sustenta a afirmação do protocolo.
- As duas metades são reveladas no fechamento. Com elas e o algoritmo público (SHA-256 em modo contador, `support.SeedStream`), qualquer pessoa recomputa os alvos de todas as tentativas e confere que eles não foram escolhidos depois da resposta.
- O alvo de cada tentativa nunca sai do servidor antes de a resposta estar travada — não há campo, API ou projeção que o exponha, e há teste que tenta obtê-lo.
- **Nenhum resultado parcial dentro da sessão** (E10, trava 1): durante a sessão a pessoa vê quantas tentativas faltam, nunca quantas acertou. O desfecho aparece de uma vez, no fim.
- Sessão abandonada é registrada como abandonada e **entra no denominador** da análise do pool conforme a regra pré-registrada. Desistir de uma sessão ruim não melhora o número agregado.

**Por que este teste, apesar das evidências**: as replicações grandes dos achados de Bem deram nulo (Ritchie, Wiseman & French, 2012; Galak et al., 2012), e a página da plataforma diz isso. O valor dele aqui não é a expectativa de efeito: é ser o único protocolo em que o vazamento sensorial é **impossível por construção** — no instante da resposta o alvo ainda não foi calculado — e em que não existe julgamento subjetivo nenhum. Serve de demonstração limpa do método de verificação.

**Pool público é exploratório.** O autoatendimento anônimo não tem controle de quem testa, quantas vezes nem em que condições; os dados públicos são abertos e rotulados **exploratórios**. Um resultado confirmatório exige estudo pré-registrado com participantes identificados, N fixo e pool declarado antes da coleta — a política de rótulo já existente é quem decide.

**Verificação na mão de quem testou**: a página de resultado traz a semente num campo editável e um botão que, **no navegador da pessoa e sem falar com o servidor**, faz duas conferências. Primeira, refaz o compromisso — `SHA-256(semente | protocolo | tentativas | alternativas | banco)` — e compara com o que foi publicado no início; é isso que liga a semente revelada no fim àquela que já estava valendo antes da primeira resposta. Segunda, recomputa os alvos e compara com os gravados. Sem a primeira conferência a segunda não vale nada: qualquer semente gera "os alvos certos" se tiver sido escolhida depois das respostas. O código do verificador é servido como arquivo legível (`/js/verificador-alvos.js`). Isso tem alcance limitado e a página diz qual: prova que a semente revelada gera os alvos gravados, não que a aplicação é honesta — para isso a regra precisa ser reimplementada por fora, e é por isso que ela cabe em oito linhas e tem **vetor de teste público** (semente de 64 zeros, 12 tentativas, 4 alternativas → 1, 1, 3, 3, 2, 3, 2, 1, 1, 4, 1, 3), fixado em teste tanto na implementação em Java quanto no verificador.

**A conferência de ordem é automática**: o navegador do participante guarda o compromisso no momento em que recebe a tela da entropia — antes de sortear a metade dele — e o verificador compara, no fim, com o que a página mostra. Quem quiser a prova à mão ainda pode copiar o valor, que aparece em todas as telas de tentativa. Quando a sessão começou noutro navegador, em janela privativa ou com armazenamento bloqueado, essa conferência não existe, e a página diz isso em vez de omitir.

**Aceite**: nenhuma tentativa existe antes de o participante contribuir com a metade dele, recusado também pelo banco; as duas metades reveladas reproduzem a semente efetiva, e ela reproduz exatamente os alvos gravados; a metade do servidor confere com o compromisso publicado antes de a outra existir; o alvo não é obtível por nenhuma via antes da resposta travada; durante a sessão nenhuma tela expõe função dos escores; duas sessões com a mesma semente e as mesmas respostas dão o mesmo resultado; e o verificador do navegador acusa divergência quando a semente é alterada num único dígito.

### E12. Visão remota com quatro alvos (P3)

**Objetivo**: um teste de resposta livre, também integralmente automatizado, mais próximo do que a literatura chama de *free-response*.

**Desenho**: o sistema sorteia um alvo entre as imagens de um banco versionado. A pessoa escreve a impressão dela **antes** de ver qualquer candidata. Só então o sistema apresenta quatro imagens — o alvo e três chamarizes, sorteados da mesma forma — e a pessoa as ordena da mais parecida com a impressão à menos parecida. Acerto = alvo em primeiro lugar; acaso = 25%, declarado.

- A impressão escrita é **travada antes** de as candidatas aparecerem, e a trilha registra o hash dela. Sem isso, a pessoa pode reescrever a impressão depois de ver as imagens, que é a objeção padrão ao autojulgamento.
- O alvo e os três chamarizes saem da mesma semente comprometida, pelo mesmo mecanismo do P2.
- O banco de imagens é **versionado**, e a versão usada fica gravada na sessão: trocar as imagens depois não muda o que foi analisado.
- Vale a mesma regra de resultado parcial, de sessão abandonada e de pool exploratório do E11.

**Limite declarado**: quem julga a correspondência é a própria pessoa, e isso reintroduz a crítica de "como se pontua" que o P2 não tem. É o preço do formato de resposta livre, e fica escrito na página do protocolo. A estatística é de ordenação, com acaso exato.

**Aceite**: a impressão não pode ser alterada depois de as candidatas aparecerem; a semente revelada reproduz alvo e chamarizes; e a versão do banco de imagens usada na sessão é a que o pacote de verificação exporta.

### E13. Leitura estruturada automatizada (P4)

**Objetivo**: um protocolo AIR que roda inteiro pelo site, do cadastro ao relatório, sem ninguém da equipe no circuito — e sem que ninguém, nem o consulente, julgue se uma afirmação "encaixa".

**Ideia central**: o consulente e o médium respondem ao **mesmo questionário fechado** sobre o falecido. O consulente responde porque conheceu a pessoa; o médium responde conhecendo só o primeiro nome. O sistema confere campo a campo. Não há redator, porque não há texto livre para dividir em itens, e não há avaliação subjetiva, porque a comparação é de igualdade entre opções. Isso elimina a principal fonte de viés da avaliação pelo consulente — a tendência de encontrar sentido em afirmações genéricas — e dispensa a IA, que o P10 proíbe na pontuação.

#### Perfis e contas

| Perfil | Como se obtém | O que faz |
|---|---|---|
| **Consulente** | Escolhido ao criar a conta | Faz o pedido de consulta: primeiro nome do falecido e respostas ao questionário |
| **Médium** | Escolhido ao criar a conta | Recebe pedidos com o primeiro nome e responde ao questionário |
| **Administrador** | Configuração do servidor (`AIRLAB_RESEARCHER_USER`, `AIRLAB_RESEARCHER_PASSWORD_HASH`) | Conduz o estudo, recebe os avisos, revisa sinalizações; nunca vê respostas, nomes nem notas antes do desmascaramento |

- **Criar conta**: nome, e-mail, senha (mínimo de 10 caracteres), perfil e aceite do termo na versão corrente (E10, trava 6). A conta só vale depois da **confirmação do e-mail** por link de uso único, expirável em 48 horas e guardado apenas como hash (E10, trava 3).
- **Perfil fixo**: escolhido uma vez, nunca muda — imposto por *trigger* no banco, não só no código. É a forma mais simples da trava 2 do E10: com um mesmo e-mail ninguém é médium e consulente, em estudo nenhum.
- **Uma conta por e-mail**, comparada pelo índice cego (HMAC) do endereço, sem guardar o e-mail em claro. Contas ligadas por outros sinais são **sinalizadas** ao administrador, não bloqueadas em silêncio.
- **Nenhum caminho do site cria administrador.** O formulário aceita só consulente ou médium.
- **Dados da conta no esquema `identity`**, cifrados; a pesquisa trabalha com o código pseudônimo (`CON-…`, `MED-…`). Só o módulo `participant` lê a conta.
- **Desistência a qualquer momento**, sem justificativa: os dados identificáveis são apagados e o código pseudônimo permanece, marcado como inativo (P6).
- **Contato humano**: a página de inscrição mostra o canal da pessoa responsável (E10, trava 5). Sem esse canal e sem envio de e-mail configurado, a inscrição fica fechada no perfil de produção.

#### Fluxo

```
[1] Consulente cria a conta e confirma o e-mail
        │
[2] Consulente faz o pedido: primeiro nome do falecido + questionário
    → respostas travadas; a trilha recebe SHA-256(respostas canônicas || sal)   ◄── COMPROMISSO
        │
[3] Sistema pareia pedidos travados (mesmo gênero, faixa etária e período) e sorteia o médium do par
        │
[4] Médium recebe o pedido: os dois primeiros nomes do par, em ordem sorteada, e o questionário em branco
        │
[5] Médium responde a cada campo, ou marca "não recebi"; ao enviar, a leitura trava com hash na trilha
        │
[6] Atingido o N pré-registrado: desmascaramento automático, revelação dos sais e da semente
        │
[7] Conferência automática, análise pré-registrada e relatório
        │
[8] Devolutiva: cada consulente vê a leitura do seu alvo e a nota; o médium vê as notas das próprias leituras
```

- **O compromisso leva sal.** Respostas fechadas têm pouca entropia: o hash puro de meia dúzia de opções se quebra por força bruta, e a trilha é pública. O sal (32 bytes de `SecureRandom`) fica em custódia cifrada e é revelado no desmascaramento, junto com as respostas.
- **Sem pedido travado, nada é liberado ao médium.** Imposto no banco: não existe linha de pedido ao médium cujo pedido de consulta não esteja travado.
- **Prazo do médium** pré-registrado (padrão: 14 dias). Pedido vencido é registrado como vencido e reenviado a outro médium sorteado da mesma semente; o vencimento fica na trilha.

#### Conferência automática

Para cada par (A, B), com a leitura `R_A` (feita com o nome de A) e `R_B`:

- **Campos comparáveis** do par: os campos pontuáveis que **os dois** consulentes responderam (nenhum dos dois marcou "não sei"). O conjunto é o mesmo para alvo e chamariz, para que nenhum dos dois tenha mais chances de acerto.
- `s(R, X)` = número de campos comparáveis em que a resposta da leitura `R` é igual à do consulente `X`. "Não recebi" nunca acerta.
- **Diferença do par**: `d = [s(R_A, A) − s(R_A, B)] + [s(R_B, B) − s(R_B, A)]`.
- **Análise primária**: Wilcoxon exato unilateral sobre os `d` dos pares (a implementação de `analysis` já existe). **Secundárias**: binomial exato do sinal de `d` contra 0,5 e permutação das leituras entre todos os alvos do estudo, dentro dos estratos do pareamento.
- **Campos fora da nota**: gênero (o primeiro nome o entrega) e os critérios de pareamento — faixa etária e período do falecimento —, que são iguais dentro do par por construção e não discriminam nada. Eles são coletados, servem ao pareamento e aparecem na devolutiva.
- **Por que o chamariz é indispensável**: o número de acertos, sozinho, não diz nada. "Morreu do coração" ou "era idoso" acertam com qualquer pessoa porque são frequentes. Comparando a mesma leitura com o alvo e com o chamariz de perfil semelhante, a frequência de cada resposta se cancela, e o que sobra é o que se ajusta ao alvo **mais** do que ao chamariz.
- **Acaso declarado**: sob a hipótese nula, `d` é simétrico em torno de zero; entre os pares com `d ≠ 0`, a chance de `d > 0` é 50%.
- A regra é determinística, pública e reproduzível pelo pacote de verificação. **Nenhum texto livre entra na nota** (P10).

#### Telas por perfil

| Perfil | Tela | Conteúdo |
|---|---|---|
| Público | Página do protocolo | O que é o P4, como participar, o que o resultado não sustenta |
| Público | Criar conta | Nome, e-mail, senha, perfil (consulente ou médium), termo, contato humano |
| Público | Confirmar e-mail | Resultado do link de confirmação |
| Consulente | Painel | Situação do pedido: rascunho, travado, aguardando par, enviado ao médium, leitura recebida, aguardando o fechamento do estudo, devolutiva |
| Consulente | Novo pedido | Primeiro nome do falecido e questionário; revisão e confirmação antes de travar |
| Consulente | Devolutiva | Depois do desmascaramento: as respostas do médium ao lado das suas e a nota, com linguagem cuidadosa |
| Médium | Painel | Pedidos recebidos e prazos; leituras enviadas, sem nota até o desmascaramento |
| Médium | Leitura | Os dois primeiros nomes do par e o questionário; revisão antes de travar |
| Consulente e médium | Minha conta | Dados, termo aceito, excluir conta |
| Administrador | Painel | Contas por perfil e situação; pedidos e leituras por etapa; progresso até o N — **contagens, nunca escores** |
| Administrador | Avisos | Conclusão de cada etapa (abaixo) |
| Administrador | Sinalizações | Contas ligadas para revisão humana |
| Administrador | Estudo | Pré-registro, questionário e congelamento; relatório depois do desmascaramento |

#### Avisos ao administrador

Um aviso por etapa concluída: conta confirmada, pedido travado, par formado e enviado ao médium, leitura recebida, prazo vencido e reenvio, sinalização de contas ligadas, N atingido e estudo desmascarado, relatório pronto. Aparecem no painel e, quando há envio de e-mail configurado (`AIRLAB_ADMIN_EMAIL`), também por e-mail. **Um aviso contém só tipo, código pseudônimo e hora** — nunca nome, resposta ou nota, porque e-mail e painel não são canais cegos.

O médium recebe e-mail quando chega um pedido; o consulente, quando a devolutiva está disponível.

#### Matriz de visibilidade do P4

| Informação ↓ / Papel → | Administrador | Médium | Consulente |
|---|---|---|---|
| Identidade das contas | ✓ | só a própria | só a própria |
| Respostas do consulente ao questionário | ✗ até o desmascaramento | ✗ | só as próprias |
| Primeiro nome do falecido | ✗ | ✓ (só dos pedidos recebidos) | só o próprio |
| Ligação pedido → médium | ✗ até o desmascaramento | só os próprios pedidos | ✗ até o desmascaramento |
| Leitura do médium | ✗ até o desmascaramento | só as próprias | ✗ até o desmascaramento |
| Notas e diferenças | ✗ até o desmascaramento | ✗ até o desmascaramento | ✗ até o desmascaramento |
| Sais e semente | ✗ até a revelação | ✗ | ✗ |

**As notas não existem antes do desmascaramento**: a conferência só roda depois dele. Isso é mais forte do que escondê-las (E10, trava 1).

#### Travas próprias do P4

Além das seis travas do E10:

1. Questionário versionado, com a lista de campos fora da nota, congelado no pré-registro.
2. Respostas do consulente travadas, com compromisso salgado na trilha, **antes** da liberação do nome ao médium — imposto no banco.
3. Leitura do médium travada, com hash na trilha, antes do desmascaramento.
4. Perfil fixo imposto no banco; nenhum caminho do site cria administrador.
5. Conferência só depois do desmascaramento, por regra determinística e publicada.

#### O que o resultado do P4 não sustenta

- Não distingue sobrevivência de telepatia com os vivos: as respostas existem na memória do consulente e no banco.
- Não tem experimentador *proxy*. Nos dois estudos da comparação de Silva e Moreira-Almeida (2022) em que o médium não interagiu com ninguém, o resultado foi nulo; um nulo no P4 não refuta o P1.
- O questionário fechado só captura o que cabe nos campos. As afirmações livres e específicas, que costumam ser as mais marcantes de uma leitura, ficam fora da nota.
- O primeiro nome carrega informação (gênero, às vezes geração e região). O pareamento e os campos fora da nota reduzem esse vazamento, mas não o eliminam.
- Com inscrição aberta, o conluio entre um médium e um consulente que se conhecem e usam e-mails diferentes não é detectável com certeza. O sorteio do médium e a sinalização de contas ligadas reduzem o risco.

#### Entrega em fases

| Fase | Conteúdo | Estado |
|---|---|---|
| A | Catálogo em duas seções, páginas do P4, contas com perfil fixo, confirmação por e-mail, painéis por perfil, avisos de conta ao administrador | Implementada na SPEC 0.9 |
| B | Questionário versionado no pré-registro e pedido de consulta com compromisso salgado | Pendente |
| C | Pareamento, sorteio do médium, pedido ao médium, leitura, prazos e reenvio | Pendente |
| D | Desmascaramento automático no N, conferência, análise, relatório e devolutiva | Pendente |

**Aceite**: nenhuma conta muda de perfil, nem por SQL direto; nenhum caminho do site cria administrador; nenhum nome é liberado a médium sem pedido travado com compromisso na trilha; nenhuma nota existe antes do desmascaramento; e a conferência refeita a partir do pacote de verificação dá os mesmos `d` de cada par.

#### Anexo — rascunho do questionário, versão 1

Rascunho para revisão do pesquisador; o que vale é a versão congelada no pré-registro. Todo campo tem também "não sei" (consulente) e "não recebi" (médium).

| Campo | Opções | Na nota? |
|---|---|---|
| Gênero | feminino, masculino | não (o nome entrega) |
| Faixa etária no óbito | até 17, 18–39, 40–59, 60–79, 80 ou mais | não (pareamento) |
| Período do falecimento | até 2 anos, 2–10 anos, mais de 10 anos | não (pareamento) |
| Como foi a morte | súbita, após doença prolongada | sim |
| Causa principal | coração ou circulação, câncer, doença respiratória, acidente, violência, outra doença | sim |
| Onde faleceu | hospital, em casa, na rua ou no trânsito, outro lugar | sim |
| Estado civil | solteiro, casado ou em união, viúvo, separado | sim |
| Tinha filhos | sim, não | sim |
| Ocupação principal | campo, ofício manual ou indústria, comércio, escritório ou serviços, saúde, educação, cuidou da casa, outra | sim |
| Onde viveu a maior parte da vida | cidade grande, cidade média ou pequena, zona rural | sim |
| Temperamento | mais expansivo, mais reservado | sim |
| Estatura | abaixo da média, na média, acima da média | sim |
| Religiosidade | praticante, não praticante, sem religião | sim |
| Passatempo mais marcante | música, trabalhos manuais, esporte, leitura, cozinha, plantas, outro | sim |
| Usava óculos | sim, não | sim |
| Fumava | sim, não | sim |

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

TestSession (protocol, declared_trials, seed_commitment, server_seed_hex,
             participant_nonce, seed_hex, seed_revealed_at, pool_version)
TestSession 1──* Trial (index, response, impression_sha256, locked_at, target_index, hit)
TargetImage (pool_version, index, slug)

Account(identity schema: code, profile fixo, nome e e-mail cifrados, índice cego do e-mail,
        hash da senha, termo aceito, email_confirmed_at)
Account 1──* EmailConfirmation (token_sha256, expires_at, used_at)
AdminNotice (kind, subject_code, at)                      — sem dado pessoal, sem escore
ConsultationRequest(P4) ──1 Account(consulente), answers_commitment, locked_at     [fase B]
ReadingRequest(P4) ──1 ConsultationRequest, ──1 Account(médium), due_at            [fase C]
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

8. **Confiança no compromisso** — *resolvido para os testes públicos; em aberto para o P1*. A
   entropia do participante está implementada (E11, E12): a semente tem duas metades, o servidor
   se compromete com a dele antes de ver a do participante, e sem a metade do participante não
   existe tentativa. Para o P2 e o P3, a prova não depende mais de ordem atestada por quem opera
   a plataforma.

   Continua em aberto para o **P1**, onde o sorteio é do pesquisador e os participantes não
   estão no circuito no momento do compromisso. Ali a saída é a **ancoragem externa** da cabeça
   da cadeia de auditoria (RFC 3161 ou OpenTimestamps, já previstos no E8), que torna reescrita
   retroativa detectável por terceiros. Vale considerar também aplicar entropia de terceiros ao
   sorteio do P1 — por exemplo, do próprio consulente no momento do cadastro do alvo —, mas isso
   muda o fluxo do E3 e precisa de spec própria.

9. **P4 — questionário**: o rascunho do anexo do E13 precisa de revisão do pesquisador. Os
   critérios são: opções mutuamente exclusivas, poucas opções dominantes, nenhum campo dedutível
   do primeiro nome na nota, e linguagem cuidadosa com quem está de luto.
10. **P4 — primeiro nome ou nenhum nome**: dar ao médium só um código, sem o nome, eliminaria o
    vazamento pelo nome, mas afasta o protocolo da prática da literatura. Fica o nome, por ora,
    com o vazamento declarado.
11. **P4 — qualificação de médiuns**: uma rodada prévia com o próprio P4 (E10) antes da entrada
    no estudo confirmatório.

## 10. Próximos passos sugeridos (no Claude Code)

1. Criar o repositório e colocar `VISION.md`, `CONSTITUTION.md` e `SPEC.md` em `/docs`; referenciar os três no `CLAUDE.md`.
2. Gerar o **plano técnico** do MVP a partir desta spec (estrutura de módulos, migrações iniciais, esquemas e papéis do banco).
3. Implementar primeiro **E8 (auditoria)** e **E3 (sorteio)**: são a base de confiança de todo o resto.
4. Depois, a matriz de cegamento (seção 4) como suíte de testes, ainda antes das telas.

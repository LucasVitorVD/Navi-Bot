# Bot de Hábitos via Telegram — Especificação do Projeto

## 1. Objetivo

Automação pessoal (grupo de 3 amigos) para organizar e medir o registro de hábitos diários que hoje são feitos por foto solta no chat, de forma bagunçada e sem métricas. O bot deve:
- Lembrar os hábitos do dia em horários fixos.
- Permitir registrar cada hábito com o mínimo de esforço possível (responder à mensagem certa com uma foto).
- Consolidar um resumo diário de progresso de todos.
- Guardar histórico completo (foto + texto + dado interpretado) para uma futura plataforma web tipo "agenda".

## 2. Stack técnica (definida)

- **Linguagem/Framework**: Java + Spring Boot
- **Containerização**: Docker (Docker Compose)
- **Canal de mensagens**: Telegram Bot API (biblioteca `TelegramBots` de rubenlagus, ou equivalente em Spring)
- **IA para parsing de texto livre**: Google Gemini API (modelo Flash-Lite, free tier — ~1.500 requisições/dia, mais que suficiente)
- **Banco de dados**: SQLite (arquivo local, sem necessidade de container extra)
- **Deploy**: VM gratuita Oracle Cloud Always Free (always-on, sem cold start — necessário porque o bot precisa ficar sempre disponível pra receber mensagens)
- **Armazenamento de fotos**: disco local da VM (arquivo real, não só o file_id do Telegram)

### Decisões descartadas (com motivo, para não reconsiderar sem necessidade)
- ~~WhatsApp Cloud API oficial~~ — custo mensal, mesmo que baixo, fora do orçamento do projeto.
- ~~WhatsApp não-oficial (Baileys)~~ — risco real de banimento do número pessoal (único número do usuário); sem proteção garantida.
- ~~Discord~~ — descartado por menor aderência do grupo (muitas salas, baixa lembrança de acessar).
- ~~Email~~ — descartado por poluir a caixa de entrada.
- ~~Agente de IA genérico (ex: Hermes Agent ou similar)~~ — overkill para o caso de uso; o sistema precisa ser determinístico (regras claras), não conversacional/exploratório. IA é usada apenas de forma pontual (parsing de texto).

## 3. Regras de negócio

### 3.1 Hábitos (fixos para todos os usuários)

| Hábito | Tipo | Unidade | Meta |
|---|---|---|---|
| Água | Acumulativo | ml | 3000 |
| Estudo | Acumulativo | min | 180 |
| Cardio | Acumulativo | min | 30 |
| Alimentação boa | Binário | — | 1 registro/dia |

- Progresso que ultrapassa a meta **trava em 100%** (não soma além disso na exibição).

### 3.2 Rotina diária (horários fixos, iguais para todos)

| Horário | Ação |
|---|---|
| 07:30 | Lembrete inicial — uma mensagem por hábito, no grupo |
| 17:00 | Lembrete de reforço — avisa hábitos ainda pendentes |
| 22:00 | Resumo do dia, no grupo |

- **Janela válida de registro**: das 07:30 às 21:59:59 do mesmo dia. Registros fora dessa janela não contam para o dia.
- Lembretes de reforço (17:00): a definir se serão no grupo ou por mensagem privada (ainda em aberto — ver seção 6).

### 3.3 Mecanismo de registro

1. Às 07:30, o bot envia uma mensagem por hábito no grupo (ex: "💧 Beber 3L de água hoje").
2. O usuário responde (reply) diretamente àquela mensagem específica, anexando uma foto e, quando aplicável, uma legenda em texto livre (ex: "bebi 1 litro já hoje galera").
3. O bot identifica automaticamente **quem enviou** (ID do usuário Telegram) e **qual hábito** (pela mensagem original respondida — `reply_to_message`), sem precisar de comando ou formato rígido.
4. Cada foto/reply gera **um novo registro** (não sobrescreve o anterior) — permite múltiplos registros parciais ao longo do dia para hábitos acumulativos.

### 3.4 Interpretação da quantidade por hábito

- **Água**: cada usuário configura uma vez o tamanho da própria garrafa (ex: `/config água 500ml`). Cada foto enviada = +1 unidade daquele valor configurado. Não depende de IA.
- **Estudo / Cardio**: a legenda em texto livre é enviada à API do Gemini com um prompt que extrai a quantidade de minutos mencionada. Se não for possível extrair (retorno `null`), o bot responde pedindo à pessoa que reformule — nunca falha silenciosamente nem quebra o fluxo.
- **Alimentação boa**: binário — qualquer registro no dia já marca como cumprido (não precisa de quantidade).

### 3.5 Armazenamento de histórico (para a futura plataforma web)

Cada registro deve guardar, junto com o dado interpretado:
- A foto original (arquivo salvo localmente + referência do Telegram)
- O texto exato digitado pela pessoa (legenda)
- A quantidade que o sistema interpretou daquele texto
- Dados do usuário (nome, foto de perfil do Telegram)

Isso permite reconstruir, por pessoa e por dia, uma linha do tempo completa (tipo "agenda") sem precisar reprocessar nada — é só consultar os registros já salvos.

## 4. Modelo de dados

### `usuario`
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID/Long (PK) | |
| telegram_user_id | Long | único |
| nome | String | |
| foto_telegram_file_id | String | foto de perfil |
| criado_em | Timestamp | |

### `habito`
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| nome | String | Água, Estudo, Cardio, Alimentação |
| tipo | Enum | `BINARIO` \| `ACUMULATIVO` |
| unidade | String | "ml", "min", ou null |
| meta | Integer | 3000, 180, 30, ou null |

Tabela populada via seed fixa (não precisa de tela de configuração).

### `configuracao_usuario_habito`
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| usuario_id | FK → usuario | |
| habito_id | FK → habito | |
| valor_unidade_pessoal | Integer | ex: 500 (ml por garrafa), só usado para água |

### `registro`
| Campo | Tipo | Observação |
|---|---|---|
| id | Long (PK) | |
| usuario_id | FK → usuario | |
| habito_id | FK → habito | |
| data_referencia | Date | dia considerado (respeita janela 07:30–21:59) |
| criado_em | Timestamp | horário real do envio |
| legenda_texto | String | texto original digitado |
| quantidade_extraida | Integer | valor interpretado (ml, min, ou null p/ binário) |
| foto_telegram_file_id | String | referência no Telegram |
| foto_caminho_local | String | cópia salva em disco |
| mensagem_telegram_id | Long | id da mensagem de reply (rastreabilidade) |

## 5. Fluxo técnico de gravação de um registro

1. Telegram envia webhook ao Spring Boot com: foto (file_id), legenda, id da mensagem respondida, id do usuário.
2. Sistema identifica o `habito` a partir do `mensagem_telegram_id` do lembrete original.
3. Se `ACUMULATIVO` e não for água → envia legenda para o Gemini extrair minutos → grava `quantidade_extraida`.
4. Se for água → usa `configuracao_usuario_habito` → `quantidade_extraida` = valor fixo da garrafa daquela pessoa.
5. Se for `BINARIO` (alimentação) → grava o registro sem quantidade.
6. Baixa a foto do Telegram e salva localmente (`foto_caminho_local`), mantendo o `file_id` original.
7. Insere a linha em `registro`.

## 6. Pontos ainda em aberto (decidir antes ou durante o desenvolvimento)

- Lembrete de reforço das 17:00: enviar no grupo ou individualmente (privado) por pessoa?
- Formato exato das mensagens de lembrete e do resumo das 22:00 (texto/emoji a definir na implementação).
- Estrutura exata dos endpoints/webhook do Telegram (a detalhar na próxima etapa do desenvolvimento).

## 7. Boas práticas de desenvolvimento (orientação para a IA que for codar)

Para evitar que o código seja gerado todo de uma vez (o que dificulta revisão e aumenta risco de erro acumulado), seguir estritamente:

### 7.1 TDD (Test-Driven Development)
- Para cada funcionalidade, escrever primeiro os **testes** (unitários, e quando fizer sentido, de integração) descrevendo o comportamento esperado — **antes** de qualquer implementação.
- Só implementar o código de produção depois que os testes estiverem escritos e claramente falhando (red → green → refactor).
- Não pular etapas: não escrever teste e implementação juntos "para ganhar tempo".

### 7.2 Desenvolvimento incremental, uma funcionalidade por vez
- Não gerar o projeto inteiro de uma vez. Avançar em fatias pequenas e verificáveis, na seguinte ordem sugerida:
  1. Estrutura base do projeto (Spring Boot + Docker) sem lógica de negócio.
  2. Entidades e repositórios (`usuario`, `habito`, `configuracao_usuario_habito`, `registro`) com testes de persistência.
  3. Seed fixa dos hábitos.
  4. Lógica de identificação do hábito a partir da mensagem respondida (reply), com testes isolados, ainda sem integração real com Telegram.
  5. Integração real com a API do Telegram (recebimento de mensagens/fotos).
  6. Lógica de interpretação de quantidade (água por configuração; estudo/cardio via Gemini), com testes usando respostas simuladas (mock) da IA antes de integrar a API de verdade.
  7. Scheduler dos lembretes (07:30, 17:00) e resumo (22:00).
  8. Armazenamento local das fotos.
- Cada etapa só avança para a próxima depois de testada e revisada — não seguir para a etapa seguinte com testes quebrados ou pendentes.

### 7.3 Revisão a cada etapa
- Ao final de cada fatia (item da lista acima), pausar e apresentar o que foi feito antes de continuar, em vez de encadear tudo automaticamente sem checkpoint.
- Preferir commits pequenos e descritivos por etapa concluída.

## 8. Escopo do MVP (o que construir agora)

**Dentro do escopo:**
- Bot do Telegram funcional (grupo único, 3 usuários)
- Scheduler para lembretes (07:30, 17:00) e resumo (22:00)
- Registro de hábitos via reply + foto, com parsing por IA quando necessário
- Modelo de dados completo (usuário, hábito, configuração, registro)
- Armazenamento de fotos localmente

**Fora do escopo (fase 2, futura):**
- Plataforma web para visualização tipo "agenda"
- Métricas avançadas (streaks, ranking semanal)

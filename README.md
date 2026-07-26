# navi

Bot de hábitos via Telegram para um grupo fechado de amigos. Ver `especificacao-projeto.md` para o escopo completo.

**Status:** Etapa 7/8 — scheduler dos lembretes (07:30, 17:00) e resumo diário (22:00).

## Rodando via Docker Compose

1. Copie `.env.example` para `.env` e preencha `TELEGRAM_BOT_TOKEN`/`GEMINI_API_KEY`/`TELEGRAM_GROUP_CHAT_ID` para as integrações reais funcionarem (sem elas, a aplicação sobe normalmente com essas integrações desabilitadas).
2. `docker compose up --build`
3. Aplicação sobe em `http://localhost:8080`. O arquivo SQLite é persistido em `./data/navi.db` e fotos futuras em `./fotos/` (fora do container).

## Rodando localmente sem Docker

Requer Java 21.

```
./mvnw spring-boot:run
```

## Testes

```
./mvnw test
```

## Modelo de dados (Etapa 2)

Entidades em `com.project.navi.domain`, repositórios Spring Data JPA em `com.project.navi.repository`. Nomenclatura em inglês para uniformidade com o restante do código:

| Entidade | Tabela | Equivalente na especificação |
|---|---|---|
| `User` | `users` | `usuario` |
| `Habit` | `habits` | `habito` |
| `UserHabitConfig` | `user_habit_configs` | `configuracao_usuario_habito` |
| `HabitRecord` | `habit_records` | `registro` |

`spring.jpa.hibernate.ddl-auto=update` cria/atualiza o schema automaticamente (o projeto ainda não usa ferramenta de migration como Flyway/Liquibase).

## Seed de hábitos (Etapa 3)

`com.project.navi.seed.HabitSeeder` roda como `ApplicationRunner` no startup e popula a tabela `habits` com os 4 hábitos fixos, de forma idempotente (só insere se a tabela estiver vazia). Os nomes exibidos (`"Água"`, `"Estudo"`, `"Cardio"`, `"Alimentação boa"`) ficam em português, já que são conteúdo enviado ao grupo no Telegram — a regra de nomenclatura em inglês vale para tabelas/colunas/classes, não para dados de domínio voltados ao usuário.

## Identificação do hábito por reply (Etapa 4)

`com.project.navi.domain.HabitReminderMessage` guarda a relação entre o `telegram_message_id` do lembrete enviado (uma mensagem por hábito, às 07:30) e o `Habit` correspondente. `com.project.navi.reminder.HabitIdentificationService.identifyHabit(Long)` recebe o id da mensagem original respondida (`reply_to_message`) e resolve para o `Habit`, sem exigir comando ou formato rígido — retorna vazio se a reply não corresponder a nenhum lembrete conhecido. Testado de forma isolada (Mockito), sem integração real com Telegram (Etapa 5).

## Integração com o Telegram (Etapa 5)

`com.project.navi.telegram`:
- `NaviTelegramBot` / `TelegramBotConfiguration` — registra o bot via **long polling** (biblioteca `telegrambots-springboot-longpolling-starter`). Long polling foi escolhido em vez de webhook porque dispensa domínio/TLS na VM Oracle Free, mantendo a simplicidade do deploy.
- `HabitReplyUpdateConsumer` — recebe cada `Update`, ignora o que não for reply-com-foto, identifica o hábito (Etapa 4) e o usuário (auto-registrando se for a primeira interação), e grava um `HabitRecord`. `extractedQuantity` e `localPhotoPath` ficam `null` por enquanto — preenchidos nas Etapas 6 e 8.
- O bot só é registrado se `TELEGRAM_BOT_TOKEN` estiver configurado (não em branco). Sem token, a aplicação sobe normalmente com a integração desabilitada; com um token inválido, a falha continua explícita no startup.
- Testado de forma isolada (Mockito + `ApplicationContextRunner`), sem depender de rede real — `telegrambots.enabled=false` em teste evita qualquer tentativa de registro contra a API do Telegram.
- `TelegramUpdateDispatcher` roteia updates de texto `/config` para `HabitConfigCommandConsumer`; o resto vai para `HabitReplyUpdateConsumer`.
- `TelegramReplySender` envia mensagens em reply (usado para pedir configuração/reformulação); com `TelegramClient` ausente (sem token), apenas loga e segue, sem quebrar quem depende dele.

## Interpretação de quantidade (Etapa 6)

`com.project.navi.quantity.HabitQuantityInterpreter` despacha por unidade do hábito:
- **Água (`ml`)**: busca `UserHabitConfig` do usuário (sem IA). Configurado via comando `/config água <valor>ml` (`com.project.navi.telegram.HabitConfigCommandConsumer`), que cria ou atualiza o valor.
- **Estudo/Cardio (`min`)**: envia a legenda para o Gemini (`com.project.navi.quantity.GeminiApiMinutesExtractor`, modelo `gemini-2.5-flash-lite` via `generateContent` com `responseSchema` JSON) e extrai os minutos.

Em ambos os casos, se a quantidade não puder ser determinada, o `HabitReplyUpdateConsumer` **não salva o registro** e responde pedindo para configurar/reformular — nunca falha silenciosamente. Testado com a IA mockada (`HabitQuantityInterpreterTest`) antes de integrar a API real, que por sua vez é testada via `MockRestServiceServer` sem precisar de chave real; falhas de rede/parsing retornam vazio em vez de lançar exceção.

## Scheduler dos lembretes (Etapa 7)

`com.project.navi.scheduler.HabitReminderScheduler` — três `@Scheduled` com cron fixo em `America/Sao_Paulo` (via `com.project.navi.time.ClockConfiguration`, também usado para corrigir um bug real: `HabitReplyUpdateConsumer` calculava `referenceDate` no fuso padrão do sistema, provavelmente UTC na VM, o que faria replies enviadas à noite em Brasília caírem no dia seguinte):

- **07:30**: uma mensagem por hábito no grupo. Cada envio grava um `HabitReminderMessage` real (`telegram_message_id` + hábito + data) — até aqui só existia a leitura (Etapa 4).
- **17:00**: reforço no grupo listando, por pessoa, os hábitos ainda pendentes (`com.project.navi.progress.HabitProgressCalculator` soma o progresso do dia, travando em 100%). Enviado no grupo, não por DM — decisão explícita, já que DM só funciona se a pessoa já tiver iniciado conversa privada com o bot.
- **22:00**: resumo do dia (progresso de todos) + frase motivacional de anime (`com.project.navi.quote.AnimeChanQuoteProvider`, via `api.animechan.io`, gratuita e sem chave — se falhar, o resumo sai sem a frase).

Sem `TELEGRAM_GROUP_CHAT_ID` configurado, os três métodos não fazem nada (mesmo padrão de degradação graciosa das Etapas 5/6). Testado via orquestração mockada (`HabitReminderSchedulerTest`) e formatação de mensagens isolada (`HabitReminderMessageFormatterTest`), sem depender de cron real disparar durante os testes.

## Estrutura de pacotes prevista (próximas etapas)

- `com.project.navi.foto` (ou similar) — Etapa 8, armazenamento local das fotos

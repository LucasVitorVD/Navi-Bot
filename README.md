# navi

Bot de hábitos via Telegram para um grupo fechado de amigos. Ver `especificacao-projeto.md` para o escopo completo.

**Status:** Etapa 5/8 — integração real com a API do Telegram (recebimento de mensagens/fotos).

## Rodando via Docker Compose

1. Copie `.env.example` para `.env` (as chaves `TELEGRAM_BOT_TOKEN`/`GEMINI_API_KEY` ainda não são usadas nesta etapa — serão consumidas a partir das Etapas 5 e 6).
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

## Estrutura de pacotes prevista (próximas etapas)

- `com.project.navi.ai` — Etapa 6
- `com.project.navi.scheduler` — Etapa 7

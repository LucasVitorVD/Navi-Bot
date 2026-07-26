# navi

Bot de hábitos via Telegram para um grupo fechado de amigos. Ver `especificacao-projeto.md` para o escopo completo.

**Status:** Etapa 3/8 — seed fixa dos hábitos (Água, Estudo, Cardio, Alimentação boa).

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

## Estrutura de pacotes prevista (próximas etapas)

- `com.project.navi.telegram` — Etapa 5
- `com.project.navi.ai` — Etapa 6
- `com.project.navi.scheduler` — Etapa 7

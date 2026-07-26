# navi

Bot de hábitos via Telegram para um grupo fechado de amigos. Ver `especificacao-projeto.md` para o escopo completo.

**Status:** Etapa 1/8 — estrutura base do projeto (Spring Boot + Docker), sem lógica de negócio.

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

## Estrutura de pacotes prevista (próximas etapas)

- `com.project.navi.domain` / `.repository` — Etapa 2
- `com.project.navi.telegram` — Etapa 5
- `com.project.navi.ai` — Etapa 6
- `com.project.navi.scheduler` — Etapa 7

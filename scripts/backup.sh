#!/usr/bin/env bash
set -euo pipefail

# Backup do banco SQLite (data/navi.db) + das fotos (fotos/), com envio pro Google Drive.
#
# Pré-requisitos (uma vez só, na máquina que for rodar isso):
#   - Docker instalado (só é usado aqui pra tirar o snapshot do SQLite com segurança,
#     via sqlite3.Connection.backup() do Python, mesmo com o app escrevendo ao mesmo tempo)
#   - rclone instalado e configurado com um remote chamado "gdrive" (rode `rclone config`
#     e siga o fluxo de login do Google Drive)
#
# Uso: agendar via cron a partir da raiz do projeto (onde fica o docker-compose.yml), ex:
#   0 3 * * * /caminho/navi/scripts/backup.sh >> /caminho/navi/backups/backup.log 2>&1

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATA_DIR="$PROJECT_DIR/data"
FOTOS_DIR="$PROJECT_DIR/fotos"
BACKUP_DIR="$PROJECT_DIR/backups"
DATE="$(date +%F_%H%M%S)"
RCLONE_REMOTE="gdrive:navi-backups"
KEEP_DAYS=14

mkdir -p "$BACKUP_DIR"
STAGE_DIR="$(mktemp -d)"
trap 'rm -rf "$STAGE_DIR"' EXIT

echo "[$(date +%T)] Tirando snapshot do banco..."
docker run --rm -v "$DATA_DIR:/data:ro" -v "$STAGE_DIR:/stage" python:3-slim python3 -c "
import sqlite3
src = sqlite3.connect('/data/navi.db')
dst = sqlite3.connect('/stage/navi.db')
src.backup(dst)
dst.close()
src.close()
"

echo "[$(date +%T)] Empacotando banco + fotos..."
cp -r "$FOTOS_DIR" "$STAGE_DIR/fotos"
ARCHIVE="$BACKUP_DIR/navi-backup-$DATE.tar.gz"
tar czf "$ARCHIVE" -C "$STAGE_DIR" navi.db fotos

echo "[$(date +%T)] Enviando pro Google Drive ($RCLONE_REMOTE)..."
rclone copy "$ARCHIVE" "$RCLONE_REMOTE"

echo "[$(date +%T)] Removendo backups com mais de $KEEP_DAYS dias (local e Drive)..."
find "$BACKUP_DIR" -name 'navi-backup-*.tar.gz' -mtime "+$KEEP_DAYS" -delete
rclone delete --min-age "${KEEP_DAYS}d" "$RCLONE_REMOTE" 2>/dev/null || true

echo "[$(date +%T)] Backup concluído: $ARCHIVE"

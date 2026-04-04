#!/bin/bash
# SyncJam Server - Build und Deploy Script
# Dieses Script baut den Server auf dem lokalen Rechner und kopiert das JAR auf den VServer.
# Ausfuehren von: E:/GitHubRepos/SyncJam/
#
# VORAUSSETZUNGEN:
#   - Java 21+ installiert
#   - SSH-Zugang zum VServer eingerichtet (am besten SSH-Key)
#   - Docker + Docker Compose auf dem VServer installiert
#
# ANPASSUNGEN:
#   Passe SERVER_USER, SERVER_HOST und SERVER_PATH unten an!

set -e

echo "=== SyncJam Server Build & Deploy ==="
echo ""

# ============================================================
# KONFIGURATION — HIER ANPASSEN!
# ============================================================
SERVER_USER="root"
SERVER_HOST="159.195.63.246"   # Netcup VPS (v2202604214567447578.luckysrv.de)
SERVER_PATH="/opt/syncjam"
# ============================================================

# Schritt 1: FAT JAR bauen
echo "[1/4] Building fat JAR via Gradle shadowJar..."
./gradlew :server:shadowJar --no-daemon

JAR_FILE="server/build/libs/server.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR not found at $JAR_FILE"
    echo "Checking build/libs directory..."
    ls server/build/libs/ 2>/dev/null || echo "(no files found)"
    exit 1
fi
echo "JAR built successfully: $JAR_FILE ($(du -sh "$JAR_FILE" | cut -f1))"

# Schritt 2: JAR in Deploy-Ordner kopieren
echo "[2/4] Copying JAR to deploy folder..."
cp "$JAR_FILE" "server-deploy/syncjam-server.jar"
echo "Copied to server-deploy/syncjam-server.jar"

# Schritt 3: Dateien auf VServer kopieren
echo "[3/4] Uploading files to VServer ${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}..."
ssh "${SERVER_USER}@${SERVER_HOST}" "mkdir -p ${SERVER_PATH}"
scp server-deploy/syncjam-server.jar  "${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/"
scp server-deploy/docker-compose.yml  "${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/"
scp server-deploy/Dockerfile          "${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/"
scp server-deploy/nginx.conf          "${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/"
echo "Upload complete."

# Schritt 4: Container neu starten
echo "[4/4] Restarting Docker container on VServer..."
ssh "${SERVER_USER}@${SERVER_HOST}" "
    cd ${SERVER_PATH} && \
    docker compose down --remove-orphans && \
    docker compose build --no-cache && \
    docker compose up -d && \
    echo 'Container gestartet.' && \
    docker compose ps
"

echo ""
echo "=== Deploy abgeschlossen! ==="
echo "Server laeuft unter: http://${SERVER_HOST}:8080"
echo "Health check:        curl http://${SERVER_HOST}:8080/health"
echo "Logs anzeigen:       ssh ${SERVER_USER}@${SERVER_HOST} 'docker logs -f syncjam-server'"

# SyncJam Server — Deployment-Anleitung

Diese Anleitung beschreibt den vollstaendigen Prozess zum Deployen des SyncJam Ktor WebSocket
Sync-Servers auf einem Linux VServer via Docker.

---

## Inhaltsverzeichnis

1. [Voraussetzungen](#1-voraussetzungen)
2. [Schnellstart (3 Befehle)](#2-schnellstart)
3. [Schritt-fuer-Schritt Setup](#3-schritt-fuer-schritt-setup)
4. [Produktions-Setup mit SSL](#4-produktions-setup-mit-ssl)
5. [Android App konfigurieren](#5-android-app-konfigurieren)
6. [Monitoring und Logs](#6-monitoring-und-logs)
7. [Updates einspielen](#7-updates-einspielen)
8. [Firewall konfigurieren](#8-firewall-konfigurieren)
9. [Troubleshooting](#9-troubleshooting)
10. [Manuelle Aufgaben](#10-manuelle-aufgaben)

---

## 1. Voraussetzungen

### Lokaler Rechner (Build-Maschine)
- Java 21 oder hoeher (`java -version`)
- Gradle Wrapper (`./gradlew`) — bereits im Projekt enthalten
- SSH-Client (`ssh`, `scp`)
- SSH-Zugang zum VServer (idealerweise mit SSH-Key, nicht Passwort)

### Linux VServer
- Ubuntu 22.04 LTS oder Debian 12 (empfohlen)
- Mindestens 1 GB RAM, 1 vCPU
- Docker Engine 24+ und Docker Compose Plugin 2.20+
- Offene Ports: 80, 443, 8080

### Netzwerk
- Feste IP-Adresse oder Domain, die auf die Server-IP zeigt
- Fuer SSL: Eine Domain (z.B. `sync.example.com`) ist erforderlich

---

## 2. Schnellstart

Wenn Docker bereits auf dem VServer installiert ist, reichen drei Befehle:

```bash
# 1. JAR bauen und auf VServer kopieren (lokal ausfuehren)
./gradlew :server:shadowJar
scp server/build/libs/server.jar root@YOUR_SERVER_IP:/opt/syncjam/syncjam-server.jar
scp server-deploy/Dockerfile server-deploy/docker-compose.yml root@YOUR_SERVER_IP:/opt/syncjam/

# 2. Container starten (auf dem VServer ausfuehren)
ssh root@YOUR_SERVER_IP "cd /opt/syncjam && docker compose build && docker compose up -d"

# 3. Health check
curl http://YOUR_SERVER_IP:8080/health
```

Erwartete Antwort: `{"status":"ok","timestamp":1712345678901}`

---

## 3. Schritt-fuer-Schritt Setup

### 3.1 Docker auf Ubuntu/Debian installieren

SSH auf den VServer einloggen:
```bash
ssh root@YOUR_SERVER_IP
```

Docker installieren (offizielle Methode):
```bash
# Alte Versionen entfernen
apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true

# Abhaengigkeiten installieren
apt-get update
apt-get install -y ca-certificates curl gnupg lsb-release

# Offiziellen Docker GPG Key hinzufuegen
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

# Docker Repository hinzufuegen
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker installieren
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Docker Service starten und autostart aktivieren
systemctl start docker
systemctl enable docker

# Installation verifizieren
docker --version
docker compose version
```

### 3.2 Verzeichnis auf dem VServer anlegen

```bash
mkdir -p /opt/syncjam
cd /opt/syncjam
```

### 3.3 JAR bauen (lokal auf deinem Rechner)

Im Projektverzeichnis `E:/GitHubRepos/SyncJam/` ausfuehren:

```bash
# Unter Linux/Mac:
./gradlew :server:shadowJar

# Unter Windows (Git Bash oder PowerShell):
./gradlew.bat :server:shadowJar
```

Das JAR wird erstellt unter: `server/build/libs/server.jar`

### 3.4 Dateien auf den VServer kopieren

```bash
# JAR umbenennen und kopieren
scp server/build/libs/server.jar root@YOUR_SERVER_IP:/opt/syncjam/syncjam-server.jar

# Docker-Konfigurationsdateien kopieren
scp server-deploy/Dockerfile        root@YOUR_SERVER_IP:/opt/syncjam/
scp server-deploy/docker-compose.yml root@YOUR_SERVER_IP:/opt/syncjam/
scp server-deploy/nginx.conf        root@YOUR_SERVER_IP:/opt/syncjam/
```

Alternativ kannst du das automatisierte Build-Script nutzen (nach Anpassung der Variablen):
```bash
bash server-deploy/build-and-deploy.sh
```

### 3.5 Container starten

Auf dem VServer:
```bash
cd /opt/syncjam

# Image bauen
docker compose build

# Container im Hintergrund starten
docker compose up -d

# Status pruefen
docker compose ps
```

Erwartete Ausgabe:
```
NAME              IMAGE                  COMMAND                  STATUS
syncjam-server    syncjam-server:latest  "sh -c 'java $JAVA_O…"  Up (healthy)
```

### 3.6 Logs pruefen

```bash
# Letzte 50 Zeilen anzeigen
docker logs syncjam-server --tail 50

# Live-Logs verfolgen
docker logs syncjam-server -f
```

Erwartete Ausgabe beim Start:
```
HH:mm:ss.SSS [main] INFO  Application - Application started in X.XXX seconds.
HH:mm:ss.SSS [main] INFO  Application - Responding at http://0.0.0.0:8080
```

### 3.7 Health Check

```bash
curl http://YOUR_SERVER_IP:8080/health
```

Erwartete JSON-Antwort:
```json
{"status":"ok","timestamp":1712345678901}
```

---

## 4. Produktions-Setup mit SSL

Fuer den Produktionseinsatz ist HTTPS mit einem SSL-Zertifikat dringend empfohlen.
WebSocket-Verbindungen ueber `wss://` sind verschluesselt und werden von
einigen Android-Versionen bei `ws://` blockiert.

### 4.1 Domain vorbereiten

- Sorge dafuer, dass deine Domain (z.B. `sync.example.com`) auf die VServer-IP zeigt.
- DNS-Propagation kann 1-24 Stunden dauern.

### 4.2 Certbot installieren (Let's Encrypt)

Auf dem VServer:
```bash
# Certbot installieren
apt-get install -y certbot

# Zertifikat beantragen (Port 80 muss frei und erreichbar sein!)
# Docker Container vorher stoppen, falls Port 80 belegt ist:
docker compose down

certbot certonly --standalone -d sync.example.com --non-interactive --agree-tos -m deine@email.com

# Zertifikate liegen in:
# /etc/letsencrypt/live/sync.example.com/fullchain.pem
# /etc/letsencrypt/live/sync.example.com/privkey.pem
```

### 4.3 SSL-Verzeichnis fuer Docker anlegen

```bash
mkdir -p /opt/syncjam/ssl
cp /etc/letsencrypt/live/sync.example.com/fullchain.pem /opt/syncjam/ssl/
cp /etc/letsencrypt/live/sync.example.com/privkey.pem /opt/syncjam/ssl/
chmod 600 /opt/syncjam/ssl/privkey.pem
```

### 4.4 nginx.conf anpassen

Die Datei `/opt/syncjam/nginx.conf` bearbeiten und `YOUR_DOMAIN.com` durch deine
echte Domain ersetzen:
```bash
sed -i 's/YOUR_DOMAIN.com/sync.example.com/g' /opt/syncjam/nginx.conf
```

### 4.5 Nginx-Profil starten

```bash
cd /opt/syncjam

# Mit Nginx-Proxy und SSL starten
docker compose --profile production up -d

# Alle laufenden Container anzeigen
docker compose --profile production ps
```

Der Server ist nun erreichbar unter:
- `https://sync.example.com` (HTTPS REST API)
- `wss://sync.example.com/ws/session/CODE` (WebSocket)
- `wss://sync.example.com/ws/ntp` (NTP-Sync)

### 4.6 Automatische Zertifikatserneuerung

```bash
# Cronjob fuer Erneuerung (monatlich)
echo "0 3 1 * * certbot renew --quiet && cp /etc/letsencrypt/live/sync.example.com/fullchain.pem /opt/syncjam/ssl/ && cp /etc/letsencrypt/live/sync.example.com/privkey.pem /opt/syncjam/ssl/ && docker compose -f /opt/syncjam/docker-compose.yml --profile production restart nginx" | crontab -
```

---

## 5. Android App konfigurieren

Sobald der Server laeuft, muss die Android App wissen, wohin sie sich verbinden soll.

### 5.1 Constants.kt anpassen

Datei: `app/src/main/java/com/syncjam/core/common/Constants.kt`

```kotlin
object Constants {
    // Server-Adresse — hier deine IP oder Domain eintragen!
    // Ohne SSL (Entwicklung/Test):
    const val SERVER_BASE_URL = "http://YOUR_SERVER_IP:8080"
    const val SERVER_WS_URL   = "ws://YOUR_SERVER_IP:8080"

    // Mit SSL (Produktion):
    // const val SERVER_BASE_URL = "https://sync.example.com"
    // const val SERVER_WS_URL   = "wss://sync.example.com"

    // WebSocket Endpunkte
    const val WS_SESSION_PATH = "/ws/session"
    const val WS_NTP_PATH     = "/ws/ntp"

    // REST Endpunkte
    const val API_SESSION     = "/session"
    const val API_HEALTH      = "/health"

    // Session
    const val MAX_PARTICIPANTS = 8
    const val SESSION_CODE_LENGTH = 6
}
```

### 5.2 NetworkModule.kt pruefen

Stelle sicher, dass `NetworkModule.kt` die Constants verwendet:

```kotlin
@Provides
@Singleton
fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; classDiscriminator = "type" })
    }
    install(WebSockets) {
        pingInterval = 15_000
    }
    defaultRequest {
        url(Constants.SERVER_BASE_URL)
    }
}
```

### 5.3 Cleartext Traffic (nur ohne SSL)

Wenn du keinen SSL-Proxy verwendest und HTTP statt HTTPS nutzt, muss in der
`AndroidManifest.xml` Cleartext-Traffic erlaubt werden (nur fuer Entwicklung!):

```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

Oder eine Network Security Config anlegen unter
`res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">YOUR_SERVER_IP</domain>
    </domain-config>
</network-security-config>
```

---

## 6. Monitoring und Logs

### Container-Status
```bash
# Status aller SyncJam-Container
docker compose -f /opt/syncjam/docker-compose.yml ps

# Detaillierte Infos
docker inspect syncjam-server
```

### Logs anzeigen
```bash
# Echtzeit-Logs
docker logs syncjam-server -f

# Letzte 100 Zeilen
docker logs syncjam-server --tail 100

# Logs seit einer bestimmten Zeit
docker logs syncjam-server --since 1h
```

### Ressourcenverbrauch
```bash
# CPU und RAM Verbrauch in Echtzeit
docker stats syncjam-server

# Einmalige Ausgabe
docker stats syncjam-server --no-stream
```

### Health Check manuell
```bash
# Einfacher Ping
curl -s http://YOUR_SERVER_IP:8080/health | python3 -m json.tool

# Session-API testen
curl -s -X POST http://YOUR_SERVER_IP:8080/session \
  -H "Content-Type: application/json" \
  -d '{"hostId":"test-user","hostName":"Test Host","sessionName":"Test Session"}' | python3 -m json.tool
```

---

## 7. Updates einspielen

### Methode 1: Automatisch (empfohlen)

```bash
# Auf dem lokalen Rechner
bash server-deploy/build-and-deploy.sh
```

### Methode 2: Manuell

```bash
# 1. Neues JAR bauen (lokal)
./gradlew :server:shadowJar

# 2. JAR kopieren
scp server/build/libs/server.jar root@YOUR_SERVER_IP:/opt/syncjam/syncjam-server.jar

# 3. Container neu bauen und starten (auf VServer)
ssh root@YOUR_SERVER_IP "
    cd /opt/syncjam && \
    docker compose down && \
    docker compose build --no-cache && \
    docker compose up -d
"
```

### Downtime minimieren

Fuer zero-downtime-Updates kann man das neue Image parallel bauen und dann
schnell umschalten:
```bash
ssh root@YOUR_SERVER_IP "
    cd /opt/syncjam && \
    docker compose build && \
    docker compose up -d --force-recreate
"
```

---

## 8. Firewall konfigurieren

### UFW (Ubuntu Firewall)

```bash
# UFW aktivieren (falls nicht aktiv)
ufw enable

# SSH erlauben (WICHTIG: vorher einstellen, sonst VServer nicht mehr erreichbar!)
ufw allow ssh
ufw allow 22/tcp

# HTTP und HTTPS fuer Nginx/SSL
ufw allow 80/tcp
ufw allow 443/tcp

# Direkter Zugriff auf Ktor (ohne Nginx, fuer Tests)
ufw allow 8080/tcp

# Status pruefen
ufw status verbose
```

### iptables (alternativ)

```bash
iptables -A INPUT -p tcp --dport 22 -j ACCEPT
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
```

### Hoster-Firewall

Viele VServer-Anbieter haben zusaetzlich eine Web-UI-Firewall im Control Panel.
Stelle sicher, dass dort ebenfalls die Ports 80, 443 und 8080 (TCP) freigegeben sind.
Gaengige Anbieter:
- Hetzner: Firewall-Regeln unter "Firewalls" im Cloud Console
- Contabo: Firewall-Einstellungen im Customer Control Panel
- DigitalOcean: Droplet Firewalls unter "Networking"

---

## 9. Troubleshooting

### Container startet nicht

```bash
# Logs des letzten Container-Versuchs anzeigen
docker logs syncjam-server

# Exit-Code pruefen
docker inspect syncjam-server --format='{{.State.ExitCode}}'

# JAR direkt testen
docker run --rm -p 8080:8080 syncjam-server:latest
```

Haeufige Ursachen:
- **JAR nicht gefunden**: Sicherstellen, dass `syncjam-server.jar` im selben Verzeichnis wie das `Dockerfile` liegt.
- **Port bereits belegt**: `lsof -i :8080` oder `ss -tlnp | grep 8080` ausfuehren, um den belegenden Prozess zu finden.
- **Java OOM**: `JAVA_OPTS` in `docker-compose.yml` anpassen, z.B. `-Xmx256m` bei wenig RAM.

### WebSocket-Verbindung schlaegt fehl

Symptome in der Android App: Verbindungsabbrueche, Session nicht gefunden.

Debugging-Schritte:
```bash
# WebSocket-Endpunkt mit wscat testen (npm install -g wscat)
wscat -c "ws://YOUR_SERVER_IP:8080/ws/session/ABCDEF?userId=test&displayName=Test"

# Oder mit curl
curl -v --include \
  --no-buffer \
  --header "Connection: Upgrade" \
  --header "Upgrade: websocket" \
  --header "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  --header "Sec-WebSocket-Version: 13" \
  "http://YOUR_SERVER_IP:8080/ws/session/TEST"
```

Haeufige Ursachen:
- **Falsche Session-URL**: Pruefe `Constants.kt` in der Android App.
- **Nginx WebSocket-Header fehlen**: Stelle sicher, dass `Upgrade` und `Connection` korrekt weitergeleitet werden.
- **Firewall blockiert**: `telnet YOUR_SERVER_IP 8080` zum Testen.
- **Session nicht angelegt**: Zuerst `POST /session` aufrufen, dann erst WebSocket verbinden.

### Health Check schlaegt fehl

```bash
# Direkt auf dem Server pruefen
curl -v http://localhost:8080/health

# Container-Netzwerk pruefen
docker exec syncjam-server wget -O- http://localhost:8080/health
```

### SSL-Zertifikat-Fehler

```bash
# Zertifikat-Gueltigkeit pruefen
openssl x509 -in /opt/syncjam/ssl/fullchain.pem -noout -dates

# Nginx-Konfiguration testen
docker exec syncjam-nginx nginx -t

# Nginx neu laden (ohne Neustart)
docker exec syncjam-nginx nginx -s reload
```

### Hohe CPU/RAM-Auslastung

```bash
# Aktive Sessions und Verbindungen anzeigen
docker stats syncjam-server --no-stream

# Thread-Dump des JVM-Prozesses (fuer Java-Profiling)
docker exec syncjam-server kill -3 1
```

---

## 10. Manuelle Aufgaben

Die folgenden Punkte muessen manuell erledigt werden — sie koennen nicht
automatisiert werden, da sie von deiner spezifischen Umgebung abhaengen.

### Pflicht vor dem ersten Start

- [ ] **SERVER_HOST in `build-and-deploy.sh` eintragen**
  Datei: `server-deploy/build-and-deploy.sh`, Zeile `SERVER_HOST="YOUR_SERVER_IP"`

- [ ] **Server-URL in der Android App eintragen**
  Datei: `app/src/main/java/com/syncjam/core/common/Constants.kt`
  Konstante `SERVER_BASE_URL` und `SERVER_WS_URL` auf deine Server-IP/Domain setzen.

- [ ] **Firewall-Ports freigeben**
  Auf dem VServer Ports 8080 (und 80/443 fuer SSL) oeffnen (siehe Abschnitt 8).

### Fuer den Produktionsbetrieb zusaetzlich

- [ ] **Domain auf VServer-IP zeigen lassen**
  DNS A-Record bei deinem Domain-Anbieter setzen.

- [ ] **SSL-Zertifikat beantragen**
  `certbot certonly --standalone -d DEINE_DOMAIN.com` auf dem VServer ausfuehren
  (siehe Abschnitt 4).

- [ ] **Domain in nginx.conf eintragen**
  `YOUR_DOMAIN.com` durch deine echte Domain ersetzen (2 Stellen).

- [ ] **Constants.kt auf HTTPS/WSS umstellen**
  `SERVER_BASE_URL` auf `https://...` und `SERVER_WS_URL` auf `wss://...` aendern.

- [ ] **Certbot-Cronjob fuer automatische Erneuerung einrichten**
  Zertifikat laeuft nach 90 Tagen ab — Erneuerung einplanen (siehe Abschnitt 4.6).

- [ ] **Supabase-Projekt anlegen**
  Das SQL-Schema aus `CLAUDE.md` in Supabase ausfuehren (Tabellen, RLS, Trigger).
  Supabase URL und Anon-Key in der Android App eintragen.

- [ ] **LiveKit-Account anlegen**
  LiveKit Cloud (kostenloser Free-Tier) unter https://livekit.io einrichten.
  API-Key und Secret in der Android App / dem Ktor-Server konfigurieren.

---

*Erstellt fuer das SyncJam-Projekt. Stand: April 2026.*

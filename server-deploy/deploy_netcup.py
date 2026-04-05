#!/usr/bin/env python3
"""
SyncJam Deploy Script - Netcup Server
Deployes the SyncJam server to the new Netcup VPS.
"""
import paramiko
import os
import sys
import time

SERVER_IP = "159.195.63.246"
SERVER_USER = "root"
SERVER_PASS = "1djvVTWgZ1ozUxW"
SERVER_PATH = "/opt/syncjam"
LOCAL_DEPLOY_DIR = os.path.dirname(os.path.abspath(__file__))

FILES_TO_UPLOAD = [
    "syncjam-server.jar",
    "Dockerfile",
    "docker-compose.yml",
    "nginx.conf",
]

# Extra files from project root (none currently needed)
EXTRA_FILES = []

DOCKER_INSTALL_SCRIPT = """
set -e
echo "=== Checking Docker ==="
if ! command -v docker &> /dev/null; then
    echo "Installing Docker..."
    apt-get update -qq
    apt-get install -y ca-certificates curl gnupg lsb-release
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      tee /etc/apt/sources.list.d/docker.list > /dev/null
    apt-get update -qq
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    systemctl enable docker
    systemctl start docker
    echo "Docker installed successfully"
else
    echo "Docker already installed: $(docker --version)"
fi
docker compose version
"""

def run_cmd(client, cmd, print_output=True):
    print(f"  $ {cmd[:80]}...")
    stdin, stdout, stderr = client.exec_command(cmd, get_pty=True)
    output = []
    for line in stdout:
        line = line.rstrip()
        output.append(line)
        if print_output:
            print(f"    {line}")
    exit_code = stdout.channel.recv_exit_status()
    if exit_code != 0:
        err = stderr.read().decode()
        if err:
            print(f"  STDERR: {err}")
    return exit_code, "\n".join(output)


def main():
    print("=== SyncJam Deploy to Netcup ===")
    print(f"Target: {SERVER_USER}@{SERVER_IP}:{SERVER_PATH}")
    print()

    # Connect
    print("[1/4] Connecting to server...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(SERVER_IP, username=SERVER_USER, password=SERVER_PASS, timeout=30)
    print("  Connected!")

    # Install Docker
    print()
    print("[2/4] Installing Docker (if needed)...")
    exit_code, output = run_cmd(client, DOCKER_INSTALL_SCRIPT)
    if exit_code != 0:
        print(f"  ERROR: Docker install failed (exit {exit_code})")
        client.close()
        sys.exit(1)

    # Create directory
    run_cmd(client, f"mkdir -p {SERVER_PATH}", print_output=False)

    # Upload files
    print()
    print("[3/4] Uploading files...")
    sftp = client.open_sftp()
    for filename in FILES_TO_UPLOAD:
        local_path = os.path.join(LOCAL_DEPLOY_DIR, filename)
        remote_path = f"{SERVER_PATH}/{filename}"
        if not os.path.exists(local_path):
            print(f"  WARNING: {filename} not found locally, skipping")
            continue
        size = os.path.getsize(local_path)
        print(f"  Uploading {filename} ({size/1024/1024:.1f} MB)...")
        sftp.put(local_path, remote_path)
        print(f"  Done: {remote_path}")

    # Upload extra files relative to project root
    project_root = os.path.dirname(LOCAL_DEPLOY_DIR)
    for (rel_local, remote_path) in EXTRA_FILES:
        local_path = os.path.join(project_root, rel_local)
        if not os.path.exists(local_path):
            print(f"  WARNING: {rel_local} not found, skipping")
            continue
        print(f"  Uploading {rel_local} → {remote_path}...")
        sftp.put(local_path, remote_path)
        print(f"  Done: {remote_path}")

    sftp.close()

    # Start container
    print()
    print("[4/4] Building and starting Docker container...")
    deploy_cmd = f"""
cd {SERVER_PATH} && \
docker compose down --remove-orphans 2>/dev/null || true && \
docker compose build --no-cache && \
docker compose up -d && \
echo "Container started" && \
docker compose ps
"""
    exit_code, output = run_cmd(client, deploy_cmd)
    if exit_code != 0:
        print(f"  ERROR: Deploy failed (exit {exit_code})")
        client.close()
        sys.exit(1)

    # Health check
    print()
    print("Waiting 15s for server to start...")
    time.sleep(15)
    print("Running health check...")
    exit_code, output = run_cmd(client, f"curl -s http://localhost:8080/health")

    # Firewall
    print()
    print("Configuring firewall (UFW)...")
    run_cmd(client, "ufw allow 22/tcp && ufw allow 8080/tcp && ufw allow 80/tcp && ufw allow 443/tcp && ufw --force enable && ufw status", print_output=True)

    client.close()

    print()
    print("=== Deploy complete! ===")
    print(f"Server: http://{SERVER_IP}:8080")
    print(f"Health: curl http://{SERVER_IP}:8080/health")
    print(f"Logs:   ssh root@{SERVER_IP} 'docker logs -f syncjam-server'")


if __name__ == "__main__":
    main()

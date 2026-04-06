#!/bin/bash
# EC2 initial setup script for ConsentLedger
# Tested on: Amazon Linux 2023 / Ubuntu 22.04
# Run as root or with sudo

set -euo pipefail

echo "=== ConsentLedger EC2 Setup ==="

# ── 1. Docker 설치 ────────────────────────────────────────────────
if ! command -v docker &>/dev/null; then
  echo "[1/5] Installing Docker..."
  if [ -f /etc/os-release ] && grep -q "Amazon Linux" /etc/os-release; then
    yum update -y
    yum install -y docker
    systemctl enable docker
    systemctl start docker
  else
    apt-get update -y
    apt-get install -y ca-certificates curl gnupg
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
      https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
      > /etc/apt/sources.list.d/docker.list
    apt-get update -y
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    systemctl enable docker
    systemctl start docker
  fi
  usermod -aG docker ec2-user 2>/dev/null || usermod -aG docker ubuntu 2>/dev/null || true
  echo "  Docker installed."
else
  echo "[1/5] Docker already installed — skipping."
fi

# ── 2. 프로젝트 클론 ──────────────────────────────────────────────
INSTALL_DIR="/opt/consentledger"

if [ ! -d "$INSTALL_DIR" ]; then
  echo "[2/5] Cloning repository..."
  git clone https://github.com/teriyakki-jin/ConsentLedger.git "$INSTALL_DIR"
  echo "  Cloned to $INSTALL_DIR"
else
  echo "[2/5] Repository already exists — pulling latest..."
  git -C "$INSTALL_DIR" pull --rebase
fi

# ── 3. 환경변수 파일 생성 ─────────────────────────────────────────
if [ ! -f "$INSTALL_DIR/.env" ]; then
  echo "[3/5] Creating .env from template..."
  cp "$INSTALL_DIR/.env.example" "$INSTALL_DIR/.env"
  echo ""
  echo "  ⚠️  $INSTALL_DIR/.env 파일을 편집하여 아래 값을 채우세요:"
  echo "     DB_HOST       → RDS 엔드포인트"
  echo "     DB_USERNAME   → RDS 사용자명"
  echo "     DB_PASSWORD   → RDS 비밀번호"
  echo "     JWT_SECRET    → 32자 이상 랜덤 문자열 (openssl rand -hex 32)"
  echo "     CORS_ALLOWED_ORIGINS → CloudFront 도메인 (https://xxx.cloudfront.net)"
  echo "     OPENAI_API_KEY → AI 이상 탐지용 (선택)"
  echo ""
else
  echo "[3/5] .env already exists — skipping."
fi

# ── 4. ghcr.io 로그인 안내 ────────────────────────────────────────
echo "[4/5] GitHub Container Registry login:"
echo "  Run: echo '<GITHUB_PAT>' | docker login ghcr.io -u <GITHUB_USER> --password-stdin"
echo "  Personal Access Token scope: read:packages"

# ── 5. systemd 서비스 등록 ────────────────────────────────────────
echo "[5/5] Registering systemd service..."
cat > /etc/systemd/system/consentledger.service << 'EOF'
[Unit]
Description=ConsentLedger Application
After=docker.service network-online.target
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/consentledger
ExecStart=/usr/bin/docker compose -f docker-compose.rds.yml up -d
ExecStop=/usr/bin/docker compose -f docker-compose.rds.yml down
TimeoutStartSec=120

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable consentledger
echo "  systemd service registered. Starts automatically on reboot."

echo ""
echo "=== Setup complete ==="
echo ""
echo "Next steps:"
echo "  1. Edit /opt/consentledger/.env"
echo "  2. docker login ghcr.io"
echo "  3. systemctl start consentledger"
echo "  4. curl http://localhost/actuator/health"

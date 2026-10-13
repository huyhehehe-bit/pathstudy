#!/usr/bin/env bash
#
# Dựng VPS Ubuntu 1GB cho PathStudy. Chạy MỘT LẦN, bằng root.
#
#   curl -fsSL https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/setup-server.sh | bash
#
# Hoặc tải về đọc trước rồi chạy (nên làm):
#   curl -fsSL -o setup.sh https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/setup-server.sh
#   less setup.sh && bash setup.sh
#
# Làm gì:
#   - swap 2GB   (thiếu là app bị kernel giết khi AI đọc PDF)
#   - Java 21, PostgreSQL, nginx
#   - user riêng chạy app, KHÔNG chạy bằng root
#   - Postgres chỉnh cho 1GB, chỉ nghe localhost
#   - tường lửa chỉ mở 22/80/443
#   - systemd tự bật lại app khi chết
#
# Chạy lại lần nữa cũng không sao: script bỏ qua những gì đã làm.
#
set -euo pipefail

APP_USER=pathstudy
APP_DIR=/opt/pathstudy
DB_NAME=pathstudy
DB_USER=pathstudy

say() { echo; echo "=== $* ==="; }
[[ $EUID -eq 0 ]] || { echo "Phải chạy bằng root. Thử: sudo bash $0"; exit 1; }

say "1/8 Cập nhật hệ thống"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq && apt-get upgrade -y -qq

say "2/8 Swap 2GB"
if swapon --show | grep -q .; then
  echo "  đã có swap, bỏ qua"
else
  fallocate -l 2G /swapfile && chmod 600 /swapfile
  mkswap /swapfile >/dev/null && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo 'vm.swappiness=20' > /etc/sysctl.d/99-pathstudy.conf
  sysctl -w vm.swappiness=20 >/dev/null
  echo "  xong"
fi

say "3/8 Cài gói (Java 21, PostgreSQL, nginx)"
apt-get install -y -qq openjdk-21-jre-headless postgresql postgresql-contrib \
                       nginx ufw rsync curl ca-certificates logrotate
java -version 2>&1 | head -1

say "4/8 User chạy app"
id -u "$APP_USER" &>/dev/null || useradd --system --create-home --shell /usr/sbin/nologin "$APP_USER"
mkdir -p "$APP_DIR" /var/log/pathstudy /var/backups/pathstudy
chown -R "$APP_USER:$APP_USER" "$APP_DIR" /var/log/pathstudy

say "5/8 PostgreSQL"
DB_PASS=$(head -c 24 /dev/urandom | base64 | tr -d '/+=' | head -c 24)
sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" | grep -q 1 \
  || sudo -u postgres psql -q -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';"
sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" | grep -q 1 \
  || sudo -u postgres psql -q -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"

PG_MAIN=$(ls -d /etc/postgresql/*/main | head -1)
cat > "$PG_MAIN/conf.d/pathstudy.conf" 2>/dev/null || mkdir -p "$PG_MAIN/conf.d"
cat > "$PG_MAIN/conf.d/pathstudy.conf" <<'PGCONF'
# VPS 1GB: mặc định của Postgres ăn nhiều hơn mức máy này chịu được.
listen_addresses = 'localhost'
shared_buffers = 64MB
work_mem = 4MB
maintenance_work_mem = 32MB
effective_cache_size = 256MB
max_connections = 20
PGCONF
grep -q "conf.d" "$PG_MAIN/postgresql.conf" || \
  echo "include_dir = 'conf.d'" >> "$PG_MAIN/postgresql.conf"
systemctl restart postgresql
echo "  Postgres chỉ nghe localhost, không phơi ra Internet"

say "6/8 Tường lửa"
ufw --force reset >/dev/null
ufw default deny incoming >/dev/null && ufw default allow outgoing >/dev/null
ufw allow 22/tcp >/dev/null && ufw allow 80/tcp >/dev/null && ufw allow 443/tcp >/dev/null
ufw --force enable >/dev/null
echo "  mở 22/80/443. Cổng 5432 đóng với bên ngoài."

say "7/8 Biến môi trường"
if [[ -f $APP_DIR/pathstudy.env ]]; then
  echo "  đã có, giữ nguyên (không ghi đè mật khẩu DB cũ)"
else
  cat > "$APP_DIR/pathstudy.env" <<ENVFILE
SPRING_PROFILES_ACTIVE=prod
PORT=8080

DATABASE_URL=jdbc:postgresql://localhost:5432/$DB_NAME
DATABASE_USERNAME=$DB_USER
DATABASE_PASSWORD=$DB_PASS

# ĐIỀN NỐT (lấy từ Render -> Environment)
GEMINI_API_KEY=
GA_MEASUREMENT_ID=
SIGNUP_AUTO_PREMIUM_DAYS=30
TEACHER_SIGNUP_CODE=
FEEDBACK_SHEET_WEBHOOK=
PAYMENT_ACCOUNT=
ENVFILE
  chmod 600 "$APP_DIR/pathstudy.env"
  chown "$APP_USER:$APP_USER" "$APP_DIR/pathstudy.env"
  echo "  đã tạo $APP_DIR/pathstudy.env (mật khẩu DB sinh ngẫu nhiên, nằm trong đó)"
fi

say "8/8 systemd"
cat > /etc/systemd/system/pathstudy.service <<'UNIT'
[Unit]
Description=PathStudy
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=pathstudy
WorkingDirectory=/opt/pathstudy
EnvironmentFile=/opt/pathstudy/pathstudy.env

# Cờ JVM cho 1GB RAM:
#  Xmx320m          heap vừa đủ, chừa chỗ cho Postgres + OS
#  UseSerialGC      GC nhẹ nhất khi heap nhỏ và chỉ 1 vCPU
#  TieredStopAtLevel=1  bỏ JIT tầng cao -> khởi động nhanh, ít RAM
#  MaxMetaspaceSize chặn metaspace phình không kiểm soát
ExecStart=/usr/bin/java \
  -Xms128m -Xmx320m \
  -XX:+UseSerialGC \
  -XX:TieredStopAtLevel=1 \
  -XX:MaxMetaspaceSize=128m \
  -XX:+ExitOnOutOfMemoryError \
  -Dspring.jmx.enabled=false \
  -jar /opt/pathstudy/pathstudy.jar

Restart=always
RestartSec=10
StandardOutput=append:/var/log/pathstudy/app.log
StandardError=append:/var/log/pathstudy/app.log

# Siết quyền: app không cần ghi ra ngoài thư mục của nó
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectHome=true
ReadWritePaths=/opt/pathstudy /var/log/pathstudy

[Install]
WantedBy=multi-user.target
UNIT

cat > /etc/logrotate.d/pathstudy <<'LOGROT'
/var/log/pathstudy/*.log {
    daily
    rotate 7
    compress
    missingok
    notifempty
    copytruncate
}
LOGROT

systemctl daemon-reload
systemctl enable pathstudy >/dev/null 2>&1 || true

echo
echo "================================================================"
echo " XONG PHẦN MÁY CHỦ"
echo "================================================================"
echo
echo " Mật khẩu database nằm trong: $APP_DIR/pathstudy.env"
echo
echo " TIẾP THEO:"
echo "   1. Điền GEMINI_API_KEY và các biến khác:"
echo "        nano $APP_DIR/pathstudy.env"
echo
echo "   2. Cài nginx + HTTPS:"
echo "        curl -fsSL https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/setup-nginx.sh | bash -s pathlearning.id.vn"
echo
echo "   3. Từ MÁY BẠN (không phải VPS) đưa app lên:"
echo "        bash deploy/deploy.sh"
echo
echo " Chưa có pathstudy.jar nên service chưa chạy được — đó là bình thường."
echo "================================================================"

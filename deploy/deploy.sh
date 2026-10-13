#!/usr/bin/env bash
#
# Đưa bản mới lên VPS. Chạy từ MÁY BẠN (Git Bash trên Windows cũng được),
# ở thư mục gốc của dự án:
#
#   bash deploy/deploy.sh
#
# Vì sao build ở máy bạn chứ không build trên VPS: `mvn package` với Java 21
# cần khoảng 1GB RAM chỉ riêng lúc biên dịch. Trên VPS 1GB đang chạy sẵn
# Postgres và app thì lệnh build sẽ bị kernel giết.
#
set -euo pipefail

VPS_HOST="${VPS_HOST:-103.77.240.225}"
VPS_USER="${VPS_USER:-root}"
SSH_KEY="${SSH_KEY:-}"            # để trống thì dùng khoá mặc định ~/.ssh/id_*
JAR=target/pathstudy.jar
REMOTE=/opt/pathstudy/pathstudy.jar

SSH_OPTS=(-o StrictHostKeyChecking=accept-new -o ConnectTimeout=15)
[[ -n $SSH_KEY ]] && SSH_OPTS+=(-i "$SSH_KEY")

say() { echo; echo "=== $* ==="; }

[[ -f pom.xml ]] || { echo "Chạy ở thư mục gốc dự án (chỗ có pom.xml)"; exit 1; }

say "1/4 Build"
if [[ -x .tools/maven/apache-maven-3.9.9/bin/mvn ]]; then
  JAVA_HOME=.tools/jdk/jdk-21.0.12.1+1 .tools/maven/apache-maven-3.9.9/bin/mvn -q -DskipTests package
else
  mvn -q -DskipTests package
fi
[[ -f $JAR ]] || { echo "Không thấy $JAR"; exit 1; }
SIZE=$(du -m "$JAR" | cut -f1)
echo "  $JAR — ${SIZE}MB"

say "2/4 Đưa lên VPS"
echo "  (lần đầu sẽ lâu vì jar ${SIZE}MB; những lần sau rsync chỉ gửi phần khác)"
if command -v rsync >/dev/null 2>&1; then
  rsync -avz --progress -e "ssh ${SSH_OPTS[*]}" "$JAR" "$VPS_USER@$VPS_HOST:$REMOTE.new"
else
  scp "${SSH_OPTS[@]}" "$JAR" "$VPS_USER@$VPS_HOST:$REMOTE.new"
fi

say "3/4 Đổi sang bản mới và khởi động lại"
ssh "${SSH_OPTS[@]}" "$VPS_USER@$VPS_HOST" bash -s <<'REMOTE_CMDS'
set -e
# Giữ lại bản cũ để lỡ hỏng còn quay về
[[ -f /opt/pathstudy/pathstudy.jar ]] && cp /opt/pathstudy/pathstudy.jar /opt/pathstudy/pathstudy.jar.bak
mv /opt/pathstudy/pathstudy.jar.new /opt/pathstudy/pathstudy.jar
chown pathstudy:pathstudy /opt/pathstudy/pathstudy.jar
systemctl restart pathstudy
REMOTE_CMDS

say "4/4 Chờ app lên"
OK=0
for i in $(seq 1 40); do
  CODE=$(ssh "${SSH_OPTS[@]}" "$VPS_USER@$VPS_HOST" \
         "curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1:8080/login" 2>/dev/null || echo 000)
  if [[ $CODE == 200 ]]; then OK=1; echo "  app đã chạy (sau $((i*5))s)"; break; fi
  sleep 5
done

if [[ $OK -eq 0 ]]; then
  echo
  echo "  App CHƯA lên. Xem log:"
  echo "    ssh $VPS_USER@$VPS_HOST 'tail -50 /var/log/pathstudy/app.log'"
  echo
  echo "  Quay về bản cũ nếu cần:"
  echo "    ssh $VPS_USER@$VPS_HOST 'mv /opt/pathstudy/pathstudy.jar.bak /opt/pathstudy/pathstudy.jar && systemctl restart pathstudy'"
  exit 1
fi

echo
echo "================================================================"
echo " ĐÃ TRIỂN KHAI XONG"
echo "================================================================"
ssh "${SSH_OPTS[@]}" "$VPS_USER@$VPS_HOST" \
  "free -m | head -2; echo; systemctl is-active pathstudy | sed 's/^/  service: /'"

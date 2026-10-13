#!/usr/bin/env bash
#
# nginx + HTTPS cho PathStudy. Chạy SAU setup-server.sh, bằng root.
#
#   curl -fsSL https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/setup-nginx.sh | bash -s pathlearning.id.vn
#
# Thêm www sau (khi DNS của www đã trỏ về VPS):
#   certbot --nginx -d pathlearning.id.vn -d www.pathlearning.id.vn --expand
#
# TRƯỚC KHI CHẠY: tên miền phải đã trỏ A record về IP VPS này.
#
# CÁCH LÀM: dựng cấu hình HTTP trước rồi để certbot tự thêm phần HTTPS.
# KHÔNG tự viết sẵn khối `listen 443 ssl` — lúc đó chưa có file chứng chỉ nên
# nginx sẽ không khởi động nổi, hỏng cả bước lấy chứng chỉ. (Bản đầu của
# script này mắc đúng lỗi đó.)
#
set -euo pipefail

DOMAIN="${1:-}"
[[ -n $DOMAIN ]] || { echo "Thiếu tên miền. VD: bash setup-nginx.sh pathlearning.id.vn"; exit 1; }
[[ $EUID -eq 0 ]] || { echo "Phải chạy bằng root"; exit 1; }

say() { echo; echo "=== $* ==="; }

say "Kiểm tra tên miền đã trỏ về máy này chưa"
MY_IP=$(curl -s --max-time 15 https://api.ipify.org || echo "")
DOM_IP=$(getent hosts "$DOMAIN" | awk '{print $1}' | head -1 || echo "")
echo "  IP máy này : ${MY_IP:-?}"
echo "  $DOMAIN -> ${DOM_IP:-không phân giải được}"
if [[ -n $MY_IP && -n $DOM_IP && $MY_IP != "$DOM_IP" ]]; then
  echo "  DỪNG: tên miền chưa trỏ về máy này, Let's Encrypt sẽ thất bại."
  echo "  Sửa A record rồi chạy lại. (Cloudflare thì tạm tắt mây cam.)"
  exit 1
fi

say "Cấu hình nginx (HTTP)"
cat > /etc/nginx/sites-available/pathstudy <<NGINX
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN www.$DOMAIN;

    # Khớp spring.servlet.multipart.max-file-size (25MB) + chút dư
    client_max_body_size 30M;

    location /.well-known/acme-challenge/ { root /var/www/html; }

    # File tĩnh: cho trình duyệt cache 7 ngày. Quan trọng với bộ ảnh theme
    # (~80MB) — mỗi request tiết kiệm được là một lần JVM không phải xử lý.
    location ~* \.(png|jpg|jpeg|gif|webp|svg|ico|css|js|woff2?)\$ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        expires 7d;
        add_header Cache-Control "public";
        access_log off;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        # Spring đọc các header này (server.forward-headers-strategy=FRAMEWORK)
        # để biết request gốc là HTTPS. Thiếu thì chuyển hướng sau đăng nhập
        # rơi về http:// và trình duyệt chặn.
        proxy_set_header Host              \$host;
        proxy_set_header X-Real-IP         \$remote_addr;
        proxy_set_header X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Host  \$host;

        # AI soạn giáo trình / đọc PDF có thể lâu
        proxy_connect_timeout 15s;
        proxy_send_timeout    120s;
        proxy_read_timeout    120s;
    }
}
NGINX

ln -sf /etc/nginx/sites-available/pathstudy /etc/nginx/sites-enabled/pathstudy
rm -f /etc/nginx/sites-enabled/default
mkdir -p /var/www/html
nginx -t && systemctl reload nginx
echo "  nginx đã chạy (HTTP)"

say "Lấy chứng chỉ Let's Encrypt"
export DEBIAN_FRONTEND=noninteractive
apt-get install -y -qq certbot python3-certbot-nginx

# Chỉ xin cho tên miền gốc. www thêm sau bằng --expand khi DNS của www đã đúng,
# vì nếu một tên miền trong danh sách hỏng thì CẢ yêu cầu bị từ chối.
certbot --nginx -d "$DOMAIN" \
        --non-interactive --agree-tos --register-unsafely-without-email --redirect || {
  echo
  echo "  Certbot thất bại. Thường do DNS chưa trỏ đúng hoặc Cloudflare bật mây cam."
  echo "  Sửa xong chạy lại:  certbot --nginx -d $DOMAIN"
  exit 1
}

systemctl enable certbot.timer >/dev/null 2>&1 || true

echo
echo "================================================================"
echo " HTTPS đã bật cho $DOMAIN — chứng chỉ tự gia hạn"
echo
echo " Khi DNS của www đã trỏ về VPS, thêm www bằng:"
echo "   certbot --nginx -d $DOMAIN -d www.$DOMAIN --expand"
echo
echo " Chưa có pathstudy.jar thì web trả 502 — từ MÁY BẠN chạy:"
echo "   bash deploy/deploy.sh"
echo "================================================================"

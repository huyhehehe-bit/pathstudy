#!/usr/bin/env bash
#
# nginx + HTTPS cho PathStudy. Chạy SAU setup-server.sh, bằng root.
#
#   curl -fsSL https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/setup-nginx.sh | bash -s pathlearning.id.vn
#
# TRƯỚC KHI CHẠY: tên miền phải đã trỏ A record về IP của VPS này, nếu không
# Let's Encrypt không xác thực được và sẽ thất bại.
#
set -euo pipefail

DOMAIN="${1:-}"
[[ -n $DOMAIN ]] || { echo "Thiếu tên miền. Ví dụ: bash setup-nginx.sh pathlearning.id.vn"; exit 1; }
[[ $EUID -eq 0 ]] || { echo "Phải chạy bằng root"; exit 1; }

say() { echo; echo "=== $* ==="; }

say "Kiểm tra tên miền đã trỏ về máy này chưa"
MY_IP=$(curl -s --max-time 15 https://api.ipify.org || echo "")
DOM_IP=$(getent hosts "$DOMAIN" | awk '{print $1}' | head -1 || echo "")
echo "  IP máy này : ${MY_IP:-không xác định}"
echo "  $DOMAIN -> ${DOM_IP:-không phân giải được}"
if [[ -n $MY_IP && -n $DOM_IP && $MY_IP != "$DOM_IP" ]]; then
  echo
  echo "  CẢNH BÁO: tên miền chưa trỏ về máy này."
  echo "  Let's Encrypt sẽ thất bại. Sửa A record rồi chạy lại."
  echo "  (Nếu đang dùng Cloudflare proxy thì tạm tắt mây cam khi cấp chứng chỉ.)"
  read -rp "  Vẫn tiếp tục? [y/N] " go
  [[ ${go,,} == y ]] || exit 1
fi

say "Cấu hình nginx"
cat > /etc/nginx/sites-available/pathstudy <<NGINX
# PathStudy — reverse proxy tới app Java ở cổng 8080
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN www.$DOMAIN;

    # Để certbot xác thực
    location /.well-known/acme-challenge/ { root /var/www/html; }

    location / { return 301 https://$DOMAIN\$request_uri; }
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name www.$DOMAIN;
    # chứng chỉ do certbot điền vào sau
    return 301 https://$DOMAIN\$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name $DOMAIN;

    # File tải lên tối đa — khớp spring.servlet.multipart.max-file-size (25MB)
    client_max_body_size 30M;

    # Ảnh/CSS/JS: để nginx trả thẳng và cho trình duyệt cache lâu.
    # Quan trọng với bộ ảnh theme Kawai (~80MB) — mỗi request tiết kiệm được
    # là một lần JVM không phải thức dậy trên con máy 1GB.
    location ~* \.(png|jpg|jpeg|gif|webp|svg|ico|css|js|woff2?)\$ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_cache_valid 200 7d;
        expires 7d;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        # Spring đọc mấy header này (server.forward-headers-strategy=FRAMEWORK)
        # để biết request gốc là HTTPS. Thiếu là chuyển hướng sau đăng nhập
        # rơi về http:// và trình duyệt chặn.
        proxy_set_header Host              \$host;
        proxy_set_header X-Real-IP         \$remote_addr;
        proxy_set_header X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Host  \$host;

        # AI soạn giáo trình/đọc PDF có thể lâu
        proxy_connect_timeout 15s;
        proxy_send_timeout    120s;
        proxy_read_timeout    120s;
    }
}
NGINX

ln -sf /etc/nginx/sites-available/pathstudy /etc/nginx/sites-enabled/pathstudy
rm -f /etc/nginx/sites-enabled/default
mkdir -p /var/www/html

say "Cài chứng chỉ Let's Encrypt"
apt-get install -y -qq certbot python3-certbot-nginx
# certbot tự sửa file nginx ở trên để trỏ đúng đường dẫn chứng chỉ
certbot --nginx -d "$DOMAIN" -d "www.$DOMAIN" \
        --non-interactive --agree-tos --register-unsafely-without-email \
        --redirect || {
  echo
  echo "  Certbot thất bại. Thường do tên miền chưa trỏ về máy này,"
  echo "  hoặc Cloudflare đang bật mây cam. Sửa xong chạy lại:"
  echo "    certbot --nginx -d $DOMAIN -d www.$DOMAIN"
  exit 1
}

nginx -t && systemctl reload nginx
systemctl enable certbot.timer >/dev/null 2>&1 || true   # tự gia hạn

echo
echo "================================================================"
echo " nginx + HTTPS đã xong cho $DOMAIN"
echo " Chứng chỉ tự gia hạn qua certbot.timer"
echo
echo " Giờ web sẽ trả 502 cho tới khi bạn đưa pathstudy.jar lên —"
echo " đó là bình thường. Từ MÁY BẠN chạy: bash deploy/deploy.sh"
echo "================================================================"

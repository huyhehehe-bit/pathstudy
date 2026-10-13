#!/usr/bin/env bash
#
# Sao lưu database. Cài đặt trên VPS, chạy hằng ngày bằng cron.
#
#   curl -fsSL -o /usr/local/bin/pathstudy-backup https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/backup.sh
#   chmod +x /usr/local/bin/pathstudy-backup
#   echo '15 3 * * * root /usr/local/bin/pathstudy-backup' > /etc/cron.d/pathstudy-backup
#
# CẢNH BÁO QUAN TRỌNG
# Bản sao lưu nằm TRÊN CHÍNH VPS này KHÔNG PHẢI là backup. Ổ hỏng hay nhà
# cung cấp sập là mất cả hai. Phải đem file ra ngoài — xem phần cuối file.
#
set -euo pipefail

DIR=/var/backups/pathstudy
KEEP_DAYS=14
ENV_FILE=/opt/pathstudy/pathstudy.env

mkdir -p "$DIR"

# Lấy thông tin DB từ file env của app, khỏi phải khai lại hai nơi
DB_NAME=$(grep -oP 'DATABASE_URL=.*/\K[^?]+' "$ENV_FILE" 2>/dev/null || echo pathstudy)
DB_USER=$(grep -oP '^DATABASE_USERNAME=\K.*' "$ENV_FILE" 2>/dev/null || echo pathstudy)
DB_PASS=$(grep -oP '^DATABASE_PASSWORD=\K.*' "$ENV_FILE" 2>/dev/null || echo "")

STAMP=$(date +%Y%m%d-%H%M)
OUT="$DIR/pathstudy-$STAMP.sql.gz"

PGPASSWORD="$DB_PASS" pg_dump -h localhost -U "$DB_USER" "$DB_NAME" | gzip -9 > "$OUT"

SIZE=$(du -h "$OUT" | cut -f1)
echo "$(date '+%F %T')  đã sao lưu: $OUT ($SIZE)"

# Dọn bản cũ
find "$DIR" -name 'pathstudy-*.sql.gz' -mtime "+$KEEP_DAYS" -delete

# Kiểm tra file không rỗng — dump hỏng mà vẫn tạo file là cái bẫy kinh điển
if [[ $(stat -c%s "$OUT") -lt 1000 ]]; then
  echo "CẢNH BÁO: file sao lưu quá nhỏ, nhiều khả năng pg_dump thất bại!" >&2
  exit 1
fi

# ----------------------------------------------------------------------
# ĐEM RA NGOÀI MÁY — bỏ chú thích một trong các cách dưới
#
# Cách 1: rclone lên Google Drive / Backblaze B2 (cài: apt install rclone,
#          cấu hình: rclone config)
# rclone copy "$OUT" remote:pathstudy-backup/ && echo "  đã đẩy lên remote"
#
# Cách 2: scp về máy bạn (cần khoá SSH không mật khẩu)
# scp "$OUT" user@may-cua-ban:/duong/dan/backup/
#
# Cách 3: tự tải định kỳ từ máy bạn
# scp root@103.77.240.225:/var/backups/pathstudy/pathstudy-*.sql.gz ./
# ----------------------------------------------------------------------

# Khôi phục khi cần:
#   gunzip -c pathstudy-YYYYMMDD-HHMM.sql.gz | PGPASSWORD=... psql -h localhost -U pathstudy pathstudy

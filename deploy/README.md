# Triển khai PathStudy lên VPS Ubuntu 1GB

Bộ script này dựng toàn bộ máy chủ. Viết cho **Ubuntu 24.04, 1 vCPU, 1GB RAM**.

> **Chưa được chạy thử trên máy thật.** Script viết cẩn thận và có kiểm tra ở
> từng bước, nhưng lần đầu chạy nên theo dõi output chứ đừng bỏ đi.

## Thứ tự làm

### 1. Trên VPS — dựng máy chủ (một lần)

```bash
curl -fsSL -o setup.sh https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/setup-server.sh
less setup.sh          # nên đọc trước khi chạy thứ gì bằng root
bash setup.sh
```

Xong sẽ có: swap 2GB, Java 21, PostgreSQL (đã chỉnh cho 1GB, chỉ nghe
localhost), nginx, tường lửa chỉ mở 22/80/443, user `pathstudy` riêng,
systemd service, và file `/opt/pathstudy/pathstudy.env` chứa mật khẩu DB
sinh ngẫu nhiên.

### 2. Trên VPS — điền biến môi trường

```bash
nano /opt/pathstudy/pathstudy.env
```

Chép từ Render → Environment sang: `GEMINI_API_KEY`, `GA_MEASUREMENT_ID`,
`TEACHER_SIGNUP_CODE`, `FEEDBACK_SHEET_WEBHOOK`…

Ba biến `DATABASE_*` đã điền sẵn, **đừng sửa**.

### 3. Trên VPS — nginx + HTTPS

Tên miền phải trỏ A record về IP VPS **trước** bước này.

```bash
curl -fsSL https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/setup-nginx.sh | bash -s pathlearning.id.vn
```

### 4. Trên MÁY BẠN — đưa app lên

```bash
bash deploy/deploy.sh
```

Build ở máy bạn rồi đẩy jar lên. **Không build trên VPS**: `mvn package` cần
~1GB RAM chỉ riêng lúc biên dịch, trên máy 1GB đang chạy Postgres thì sẽ bị
kernel giết.

### 5. Trên VPS — chuyển dữ liệu từ Render (nếu muốn giữ)

Lấy Internal/External Database URL trong Render → Dashboard → Database.

```bash
# xuất từ Render
pg_dump "postgresql://user:pass@host.render.com/db" > render.sql

# nạp vào VPS
psql -h localhost -U pathstudy pathstudy < render.sql
systemctl restart pathstudy
```

Không bắt buộc: nội dung bài học nằm trong `DataSeeder` và tự dựng lại.
Chỉ mất tài khoản người dùng, kết quả bài làm và nội dung giáo viên tự thêm.

### 6. Trên VPS — bật sao lưu hằng ngày

```bash
curl -fsSL -o /usr/local/bin/pathstudy-backup https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/backup.sh
chmod +x /usr/local/bin/pathstudy-backup
echo '15 3 * * * root /usr/local/bin/pathstudy-backup' > /etc/cron.d/pathstudy-backup
/usr/local/bin/pathstudy-backup     # chạy thử ngay
```

Mở `backup.sh` bỏ chú thích phần **đem ra ngoài máy**. Bản sao lưu nằm trên
chính VPS đó không phải backup — ổ hỏng là mất cả hai.

## Lệnh hay dùng

```bash
systemctl status pathstudy          # app sống không
systemctl restart pathstudy
tail -f /var/log/pathstudy/app.log  # log
free -m                             # RAM còn bao nhiêu
journalctl -u pathstudy -n 50       # lỗi lúc khởi động
```

## Ngân sách RAM 1GB

| Thành phần | RAM |
|---|---|
| Ubuntu | ~200 MB |
| nginx | ~25 MB |
| PostgreSQL (đã chỉnh) | ~150 MB |
| JVM (`-Xmx320m`) | ~300 MB |
| **Tổng** | **~675 MB** |
| Còn lại + swap 2GB | ~325 MB |

Đừng chạy thêm dịch vụ nặng nào trên máy này. Đặc biệt **không chạy
`browser_demo.py`** ở đây — mỗi trình duyệt tốn ~160MB, máy sẽ sập.

## Quay về bản cũ

`deploy.sh` luôn giữ `pathstudy.jar.bak` của lần trước:

```bash
ssh root@<IP> 'mv /opt/pathstudy/pathstudy.jar.bak /opt/pathstudy/pathstudy.jar && systemctl restart pathstudy'
```

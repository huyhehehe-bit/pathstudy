# PathStudy

Nền tảng học tập cá nhân hoá cho học sinh THPT. Học sinh chọn 1 môn, làm bài
kiểm tra đầu vào miễn phí, nhận **lộ trình học cá nhân hoá** tự điều chỉnh theo
năng lực: Chọn môn → Test đầu vào → Study Path → Học module → Bookmark →
Estimate Test → Đánh giá → Điều chỉnh lộ trình.

Môn **Ngữ văn** đã có nội dung thật (module "Phân tích tác phẩm" xoay quanh bài
thơ *Sóng* – Xuân Quỳnh). Các môn khác hiển thị trên UI, chưa có nội dung.

## Công nghệ
- **Java 21** + **Spring Boot 3.3** (Spring MVC, Thymeleaf, Spring Data JPA, Spring Security)
- **H2** file-based khi chạy local (không cần cài DB); **PostgreSQL** cho production (profile `prod`)
- Thymeleaf + CSS viết tay (không phụ thuộc CDN)

## Chạy trên máy (local)

**Cách nhanh nhất:** nhấp đúp **`run.bat`** (đã đóng gói sẵn JDK 21 + Maven trong `.tools/`, không cần cài gì).

Hoặc nếu máy đã có JDK 21 + Maven trên PATH:

```bash
mvn spring-boot:run
```

Mở trình duyệt: **http://localhost:8080**

Tài khoản demo: `demo@pathstudy.vn` / `123456` — hoặc bấm **Đăng ký** tạo tài khoản mới.

- H2 console (xem DB): http://localhost:8080/h2-console
  (JDBC URL: `jdbc:h2:file:./data/pathstudy`, user `sa`, không mật khẩu)
- Dữ liệu lưu ở thư mục `./data` (đã gitignore). Xoá thư mục này để reset toàn bộ.

## Build file JAR

```bash
mvn clean package
java -jar target/pathstudy.jar
```

## Thanh toán (để sau)
`src/main/java/com/pathstudy/service/PaymentService.java` là interface để trống.
Founder tự cài đặt (VNPay / MoMo / Stripe...) rồi tạo một `@Service` implement nó.

## Deploy production (PostgreSQL)
Đặt các biến môi trường và bật profile `prod`:
```
SPRING_PROFILES_ACTIVE=prod
JDBC_DATABASE_URL=jdbc:postgresql://<host>:5432/<db>
DB_USERNAME=<user>
DB_PASSWORD=<pass>
```
Có sẵn `Dockerfile` để build image chạy bất kỳ đâu (Render/Railway/VPS).

## Cấu trúc
```
domain/    Entity JPA (User, Subject, CourseModule, Lesson, LessonSection, Bookmark, ...)
repo/      Spring Data repositories
service/   Nghiệp vụ (Placement, Estimate, StudyPath, Bookmark, Learning, Payment)
web/       Controllers + DTO view models
config/    Security, DataSeeder (seed nội dung Ngữ văn + tài khoản demo)
resources/templates/  Thymeleaf (12 màn hình)
resources/static/     CSS + JS
```

## Toolchain (nếu chưa cài JDK/Maven)
Dự án đi kèm bản JDK 21 + Maven tải sẵn trong `.tools/` (nếu được thiết lập).
Cách khác — cài bằng winget:
```powershell
winget install Microsoft.OpenJDK.21
winget install Apache.Maven
```

# E2E tests (Playwright) — PathStudy

Test chạy trên **trình duyệt thật** (Chromium), thao tác đúng như học sinh:
điền form, bấm nút, nộp bài, xem kết quả — đồng thời bắt lỗi console, lỗi
JavaScript, request hỏng và HTTP >= 400.

## Chạy

```bash
cd e2e
npm install            # lần đầu
npx playwright install chromium   # lần đầu
npm test
```

Playwright **tự khởi động app** trước khi chạy (xem `webServer` trong
`playwright.config.ts`), nên không cần bật server bằng tay. Điều kiện duy nhất
là đã build jar:

```bash
mvn -DskipTests package
```

Xem báo cáo sau khi chạy:

```bash
npm run report
```

## An toàn dữ liệu

- Chạy ở **cổng 8099**, không đụng 8080/8090 của môi trường dev.
- Dùng **database H2 riêng** `data/pathstudy-e2e`, không chạm `data/pathstudy`.
- **Không bao giờ** trỏ vào Render/production.
- Mỗi test cần tài khoản sạch sẽ **tự đăng ký học sinh mới** (email có timestamp)
  thay vì dùng chung tài khoản demo — xem phần "Vì sao" bên dưới.

Muốn chạy lại từ đầu hoàn toàn:

```bash
rm -rf ../data/pathstudy-e2e* test-results
npm test
```

## Cỡ màn hình

Mỗi test chạy trên 3 project: `desktop` (1280×900), `tablet` (768×1024),
`mobile` (375×812). Test nào chỉ hợp một cỡ thì tự bỏ qua ở cỡ khác
(vd kiểm tra bottom-nav chỉ chạy ở mobile).

Chạy riêng một cỡ:

```bash
npx playwright test --project=mobile
```

## Các file test

| File | Nội dung |
|---|---|
| `01-smoke.spec.ts` | Trang công khai, tài nguyên tĩnh, chuyển hướng bảo mật |
| `02-auth.spec.ts` | Đăng ký (kèm mục tiêu), đăng nhập, đăng xuất, lỗi xác thực |
| `03-placement.spec.ts` | Bài kiểm tra đầu vào → chấm → xem lại câu sai → API giải thích |
| `04-path-exam.spec.ts` | Mục lục lộ trình, trang bài học, làm đề thi |
| `05-responsive.spec.ts` | Tràn ngang, bottom-nav mobile, sidebar thu gọn, cỡ chữ ô nhập |
| `06-admin-teacher.spec.ts` | Trang quản trị, công cụ giáo viên, phân quyền |

## Vì sao mỗi test tự đăng ký tài khoản mới

Bài kiểm tra đầu vào **chỉ cho 3 lượt mỗi môn**. Nếu mọi test dùng chung
`demo@pathstudy.vn` thì chạy một lần là hết lượt, lần sau test đỏ hàng loạt —
đúng kiểu "test chỉ chạy được một lần". Dùng tài khoản mới mỗi lần thì trạng
thái luôn sạch và chạy lại bao nhiêu lần cũng được.

Học sinh mới còn được tự cấp Premium (`SIGNUP_AUTO_PREMIUM_DAYS`), nên mở được
cả đề premium.

## Hai điều về hành vi của app mà test dựa vào

1. **Lộ trình chỉ mở khoá sau khi làm bài kiểm tra đầu vào.**
   `ensurePathInitialized` chỉ được gọi từ `PlacementService.grade()`. Học sinh
   ghi danh mà chưa làm bài thì mọi chương đều khoá. Test nào cần mở bài học
   phải gọi `completePlacement()` trước.

2. **AI tắt khi không có `GEMINI_API_KEY`.** Môi trường test không có key, nên
   nút "Vì sao sai?" không hiện mà thay bằng chip nhắc bật AI. Test chấp nhận
   cả hai trạng thái để chạy được ở cả máy có key lẫn không.

## Bằng chứng khi lỗi

Khi một test trượt, Playwright tự lưu vào `test-results/`:
ảnh màn hình, video, và trace (mở bằng `npx playwright show-trace <file>.zip`
để tua lại từng bước). Một số test còn chủ động đính ảnh vào báo cáo kể cả khi
qua, để đối chiếu giao diện.

import { Page, expect, TestInfo } from '@playwright/test';

/** Tài khoản seed sẵn trong DataSeeder. Chỉ dùng cho môi trường test. */
export const ACCOUNTS = {
  student: { email: 'demo@pathstudy.vn', password: '123456' },
  teacher: { email: 'teacher@pathstudy.vn', password: 'teacher123' },
  admin: { email: 'admin@pathstudy.vn', password: 'admin123' },
};

/** Email duy nhất cho mỗi lần chạy, tránh đụng tài khoản cũ khi test lại. */
export function uniqueEmail(prefix = 'e2e'): string {
  return `${prefix}_${Date.now()}_${Math.floor(Math.random() * 1000)}@e2e.local`;
}

/**
 * Thu thập lỗi console + request hỏng của một trang.
 *
 * Bỏ qua lỗi tải font/ảnh từ CDN ngoài: máy chạy test có thể không ra được
 * Internet, đó không phải lỗi của ứng dụng.
 */
export type PageProblems = {
  consoleErrors: string[];
  pageErrors: string[];
  failedRequests: string[];
  badResponses: string[];
};

const EXTERNAL = /fonts\.googleapis\.com|fonts\.gstatic\.com|googletagmanager\.com|google-analytics\.com/;

export function watchProblems(page: Page): PageProblems {
  const p: PageProblems = {
    consoleErrors: [], pageErrors: [], failedRequests: [], badResponses: [],
  };

  page.on('console', (msg) => {
    if (msg.type() !== 'error') return;
    const text = msg.text();
    if (EXTERNAL.test(text)) return;
    p.consoleErrors.push(text);
  });

  page.on('pageerror', (err) => p.pageErrors.push(String(err)));

  page.on('requestfailed', (req) => {
    if (EXTERNAL.test(req.url())) return;
    p.failedRequests.push(`${req.method()} ${req.url()} — ${req.failure()?.errorText}`);
  });

  page.on('response', (res) => {
    if (EXTERNAL.test(res.url())) return;
    // 3xx là chuyển hướng bình thường của Spring Security
    if (res.status() >= 400) {
      p.badResponses.push(`${res.status()} ${res.request().method()} ${res.url()}`);
    }
  });

  return p;
}

/** Khẳng định trang sạch lỗi. Gọi ở cuối mỗi test điều hướng. */
export function expectNoProblems(p: PageProblems, where: string) {
  expect(p.pageErrors, `Lỗi JavaScript ở ${where}`).toEqual([]);
  expect(p.consoleErrors, `Lỗi console ở ${where}`).toEqual([]);
  expect(p.failedRequests, `Request hỏng ở ${where}`).toEqual([]);
  expect(p.badResponses, `HTTP >=400 ở ${where}`).toEqual([]);
}

/** Đăng nhập qua form thật, không chèn cookie. */
export async function login(page: Page, who: keyof typeof ACCOUNTS) {
  const { email, password } = ACCOUNTS[who];
  await page.goto('/login');
  await page.locator('input[type=email]').fill(email);
  await page.locator('input[type=password]').fill(password);
  await Promise.all([
    page.waitForURL((u) => !u.pathname.startsWith('/login')),
    page.locator('button[type=submit]').click(),
  ]);
}

/**
 * Đăng xuất. Ở cỡ điện thoại, nút đăng xuất nằm trong tấm "Thêm" nên phải mở
 * tấm đó trước — giống hệt thao tác thật của học sinh.
 */
export async function logout(page: Page) {
  const form = page.locator('form[action$="/logout"]').first();
  if (!(await form.count())) return;

  if (!(await form.isVisible())) {
    const more = page.locator('.nav-more');
    if (await more.count()) await more.click();
    await form.waitFor({ state: 'visible' });
  }
  await form.locator('button').click();
  await page.waitForURL(/loggedOut|\/login|\/$/);
}

/**
 * Tạo một học sinh MỚI rồi đăng nhập luôn.
 *
 * Vì sao cần: bài kiểm tra đầu vào chỉ cho 3 lượt mỗi môn. Nếu mọi test dùng
 * chung tài khoản demo thì chạy vài lần là hết lượt và test hỏng — đúng kiểu
 * test "chỉ chạy được một lần". Mỗi lần chạy một tài khoản mới thì luôn sạch.
 * Học sinh mới cũng được tự cấp Premium nên mở được đề premium.
 */
export async function registerFreshStudent(
  page: Page,
  opts: { grade?: string; goal?: string } = {},
): Promise<string> {
  const email = uniqueEmail('hs');
  const grade = opts.grade ?? 'Lớp 10';

  await page.goto(opts.goal ? `/register?goal=${opts.goal}` : '/register');
  await page.locator('input[name=fullName]').fill('E2E Hoc Sinh');
  await page.locator('input[name=email]').fill(email);
  await page.locator('input[name=password]').fill('e2e-Test-2026');
  await page.locator(`input[name=grade][value="${grade}"]`).check();
  await page.locator('button[type=submit]').click();
  await page.waitForURL((u) => !u.pathname.startsWith('/register'));

  return email;
}

/**
 * Bảo đảm tài khoản đã ghi danh ít nhất một môn, vì /path sẽ đá về /subjects
 * nếu chưa. Không phụ thuộc test nào chạy trước.
 */
export async function ensureEnrolled(page: Page) {
  await page.goto('/path');
  if (!page.url().includes('/subjects')) return;

  const radio = page.locator('.pick input[type=radio]:not([disabled])').first();
  await radio.check();
  await page.locator('form:has(.pick) button[type=submit]').first().click();
  await page.waitForURL((u) => !u.pathname.endsWith('/subjects'));
}

/**
 * Làm trọn một bài kiểm tra đầu vào. Cần thiết vì lộ trình chỉ được khởi tạo
 * sau khi chấm bài (StudyPathService.ensurePathInitialized gọi từ
 * PlacementService.grade) — chưa làm bài thì mọi chương đều khoá.
 */
export async function completePlacement(page: Page, subjectCode = 'van') {
  await page.goto(`/placement/${subjectCode}/test`);
  const cards = page.locator('.q-card');
  const total = await cards.count();
  if (total === 0) return false;

  for (let i = 0; i < total; i++) {
    const radios = cards.nth(i).locator('input[type=radio]');
    const n = await radios.count();
    if (n > 0) await radios.nth(i % n).check();
  }
  await page.locator('form:has(.q-card) button[type=submit]').first().click();
  await page.waitForURL(/\/result/);
  return true;
}

/** Trang không được tràn ngang — lỗi responsive hay gặp nhất. */
export async function expectNoHorizontalOverflow(page: Page, where: string) {
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
  );
  expect(overflow, `${where} bị tràn ngang ${overflow}px`).toBeLessThanOrEqual(0);
}

/** Lưu ảnh làm bằng chứng, gắn vào báo cáo HTML. */
export async function attachShot(page: Page, testInfo: TestInfo, name: string) {
  const buf = await page.screenshot({ fullPage: false });
  await testInfo.attach(name, { body: buf, contentType: 'image/png' });
}

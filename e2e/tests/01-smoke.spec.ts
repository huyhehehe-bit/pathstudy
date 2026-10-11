import { test, expect } from '@playwright/test';
import { watchProblems, expectNoProblems, expectNoHorizontalOverflow, attachShot } from './helpers';

/** Các trang công khai, không cần đăng nhập. */
const PUBLIC_PAGES = [
  { path: '/', name: 'Trang chủ' },
  { path: '/login', name: 'Đăng nhập' },
  { path: '/register', name: 'Đăng ký' },
];

test.describe('Smoke — trang công khai', () => {
  for (const pg of PUBLIC_PAGES) {
    test(`${pg.name} (${pg.path}) tải được, không lỗi, không tràn ngang`, async ({ page }, testInfo) => {
      const problems = watchProblems(page);

      const res = await page.goto(pg.path);
      expect(res?.status(), `HTTP khi mở ${pg.path}`).toBeLessThan(400);

      // Có nội dung thật, không phải trang trắng hay trang lỗi
      await expect(page.locator('body')).not.toBeEmpty();
      await expect(page).toHaveTitle(/PathStudy/);
      await expect(page.locator('text=Whitelabel Error')).toHaveCount(0);

      await expectNoHorizontalOverflow(page, pg.path);
      await attachShot(page, testInfo, `${pg.name}.png`);
      expectNoProblems(problems, pg.path);
    });
  }

  test('Trang chủ hiện đủ phần hỏi mục tiêu và dẫn sang đăng ký', async ({ page }) => {
    await page.goto('/');

    const cards = page.locator('.goal-card');
    await expect(cards, 'phải có 4 thẻ mục tiêu').toHaveCount(4);

    // Mỗi thẻ mang mã mục tiêu sang trang đăng ký
    const first = cards.first();
    const href = await first.getAttribute('href');
    expect(href, 'thẻ mục tiêu phải trỏ /register?goal=').toMatch(/\/register\?goal=/);

    await first.click();
    await expect(page).toHaveURL(/\/register\?goal=/);

    // Mục tiêu đã chọn phải được điền sẵn trong form
    const goal = page.locator('select#goal');
    await expect(goal).toHaveValue(/.+/);
  });

  test('Trang chủ có nhân vật pixel trang trí', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.px-scene svg')).toHaveCount(5);
    await expect(page.locator('.px-badge svg')).toHaveCount(4);
  });

  test('Tài nguyên tĩnh trả về đúng', async ({ page, request }) => {
    for (const asset of ['/css/app.css', '/js/app.js']) {
      const res = await request.get(asset);
      expect(res.status(), `${asset} phải trả 200`).toBe(200);
      expect((await res.text()).length, `${asset} không được rỗng`).toBeGreaterThan(100);
    }
  });

  test('Khách vãng lai vào đường dẫn lạ thì bị chặn về /login', async ({ request }) => {
    // Spring Security bắt buộc đăng nhập với mọi route chưa khai báo permitAll,
    // nên đường dẫn lạ KHÔNG trả 404 mà chuyển hướng — kiểm tra đúng hành vi đó.
    const res = await request.get('/khong-ton-tai-abc-xyz', { maxRedirects: 0 });
    expect(res.status(), 'phải là chuyển hướng 302').toBe(302);
    expect(res.headers()['location'], 'phải đẩy về trang đăng nhập').toContain('/login');
  });

  test('Trang cần đăng nhập thì bị đẩy về /login', async ({ page }) => {
    await page.goto('/path');
    await expect(page).toHaveURL(/\/login/);
  });
});

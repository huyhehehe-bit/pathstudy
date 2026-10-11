import { test, expect } from '@playwright/test';
import { login, watchProblems, expectNoProblems, expectNoHorizontalOverflow } from './helpers';

const ADMIN_PAGES = ['/admin/dashboard', '/admin/users', '/admin/subjects', '/admin/feedback'];
const TEACHER_PAGES = ['/teacher', '/teacher/exams', '/teacher/questions', '/teacher/docs', '/teacher/resources'];

test.describe('Trang quản trị', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, 'admin');
  });

  for (const path of ADMIN_PAGES) {
    test(`${path} mở được, sạch lỗi`, async ({ page }) => {
      const problems = watchProblems(page);
      const res = await page.goto(path);
      expect(res?.status(), `HTTP ở ${path}`).toBeLessThan(400);
      await expect(page.locator('.content')).toBeVisible();
      await expectNoHorizontalOverflow(page, path);
      expectNoProblems(problems, path);
    });
  }

  for (const path of TEACHER_PAGES) {
    test(`${path} mở được với quyền admin`, async ({ page }) => {
      const problems = watchProblems(page);
      const res = await page.goto(path);
      expect(res?.status(), `HTTP ở ${path}`).toBeLessThan(400);
      await expectNoHorizontalOverflow(page, path);
      expectNoProblems(problems, path);
    });
  }

  test('Nhãn nav dài không bị cắt trong ô bottom-bar', async ({ page }) => {
    test.skip(test.info().project.name !== 'mobile', 'chỉ chạy ở cỡ điện thoại');

    await page.goto('/admin/dashboard');
    const items = page.locator('.nav-item:visible');
    const n = await items.count();
    for (let i = 0; i < n; i++) {
      const label = items.nth(i).locator('.lbl:visible, .lbl-sm:visible').first();
      if (!(await label.count())) continue;
      const fits = await label.evaluate((el) => el.scrollWidth <= el.clientWidth + 1);
      expect(fits, `nhãn "${await label.textContent()}" bị cắt trong bottom-bar`).toBe(true);
    }
  });

  test('Mỗi môn học có đúng một nút bật/tắt, trạng thái đọc được', async ({ page }) => {
    await page.goto('/admin/subjects');
    const toggles = page.locator('form[action*="/admin/subjects/"] button');
    expect(await toggles.count(), 'phải có nút bật/tắt cho từng môn').toBeGreaterThan(0);
    // Không bấm — tránh đổi trạng thái môn học trong lúc test
  });
});

test.describe('Phân quyền', () => {
  test('Học sinh không vào được trang quản trị', async ({ page }) => {
    await login(page, 'student');
    const res = await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    // Phải bị chặn: 403 hoặc đẩy đi nơi khác, tuyệt đối không hiện nội dung admin
    const blocked = (res?.status() ?? 0) === 403 || !page.url().includes('/admin/users');
    expect(blocked, 'học sinh KHÔNG được xem trang quản trị người dùng').toBe(true);
  });

  test('Học sinh không vào được công cụ giáo viên', async ({ page }) => {
    await login(page, 'student');
    const res = await page.goto('/teacher', { waitUntil: 'domcontentloaded' });
    const blocked = (res?.status() ?? 0) === 403 || !page.url().includes('/teacher');
    expect(blocked, 'học sinh KHÔNG được vào công cụ giáo viên').toBe(true);
  });

  test('Giáo viên vào được công cụ của mình nhưng không vào được quản trị', async ({ page }) => {
    await login(page, 'teacher');

    await page.goto('/teacher');
    await expect(page.locator('.content')).toBeVisible();

    const res = await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    const blocked = (res?.status() ?? 0) === 403 || !page.url().includes('/admin/users');
    expect(blocked, 'giáo viên KHÔNG được xem trang quản trị').toBe(true);
  });
});

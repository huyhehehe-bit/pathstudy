import { test, expect, Page } from '@playwright/test';
import { login } from './helpers';

/**
 * Ranh giới giữa ba vai trò.
 *
 * Giáo viên và quản trị KHÔNG được làm bài của học sinh. Ẩn nút trên giao diện
 * là chưa đủ — phải chặn ở tầng bảo mật, vì gõ thẳng địa chỉ vẫn vào được.
 * Mỗi lần kiểm tra ở đây đều gõ thẳng URL, đúng kiểu người dùng lách.
 */

/** Trang của học sinh mà giáo viên/quản trị không được vào. */
const CUA_HOC_SINH = [
  '/placement/toan/test',
  '/placement/toan/result',
  '/exams',
  '/exam/1',
  '/module/1',
  '/path',
  '/progress',
  '/library',
  '/subjects',
  '/subjects/add',
  '/english',
  '/upgrade',
];

async function biChan(page: Page, path: string) {
  const res = await page.goto(path, { waitUntil: 'domcontentloaded' });
  const status = res?.status() ?? 0;
  // Chặn = 403, hoặc bị đẩy sang địa chỉ khác (SecurityConfig đẩy về /start
  // rồi /start đưa về đúng nhà của vai trò đó).
  return status === 403 || !page.url().includes(path);
}

test.describe('Phân quyền ba vai trò', () => {
  for (const path of CUA_HOC_SINH) {
    test(`Giáo viên không vào được ${path}`, async ({ page }) => {
      await login(page, 'teacher');
      expect(await biChan(page, path), `giáo viên KHÔNG được vào ${path}`).toBe(true);
    });
  }

  test('Quản trị cũng không làm bài kiểm tra được', async ({ page }) => {
    await login(page, 'admin');
    for (const path of ['/placement/toan/test', '/exam/1', '/module/1/estimate/test']) {
      expect(await biChan(page, path), `admin KHÔNG được vào ${path}`).toBe(true);
    }
  });

  test('Giáo viên bị chặn thì được đưa về công cụ của mình, kèm lời giải thích',
    async ({ page }) => {
      await login(page, 'teacher');
      await page.goto('/placement/toan/test', { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveURL(/\/teacher/);
      await expect(page.locator('.toast')).toContainText('học sinh');
    });

  test('Học sinh vẫn làm bài bình thường', async ({ page }) => {
    await login(page, 'student');
    const res = await page.goto('/placement/toan/test', { waitUntil: 'domcontentloaded' });
    expect(res?.status() ?? 0, 'học sinh phải vào được bài kiểm tra').toBeLessThan(400);
    expect(page.url(), 'học sinh không được bị đẩy đi').toContain('/placement/toan');

    await page.goto('/path');
    await expect(page.locator('.content')).toBeVisible();
  });

  test('Giáo viên không thấy mục học sinh trên thanh điều hướng', async ({ page }) => {
    await login(page, 'teacher');
    await page.goto('/teacher');
    for (const href of ['/path', '/progress', '/library', '/subjects/add', '/exams']) {
      await expect(
        page.locator(`.sidebar a[href="${href}"]`),
        `thanh bên không được có mục ${href}`,
      ).toHaveCount(0);
    }
    // Logo cũng trỏ /teacher cho giáo viên, nên chỉ đếm mục trong thanh nav.
    await expect(page.locator('.sidebar .nav-item[href="/teacher"]')).toHaveCount(1);
  });
});

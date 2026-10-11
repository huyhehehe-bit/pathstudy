import { test, expect } from '@playwright/test';
import { ACCOUNTS, uniqueEmail, login, logout, watchProblems, expectNoProblems } from './helpers';

test.describe('Đăng ký & đăng nhập', () => {
  test('Đăng nhập sai mật khẩu thì báo lỗi, không cho vào', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[type=email]').fill(ACCOUNTS.student.email);
    await page.locator('input[type=password]').fill('sai-mat-khau-hoan-toan');
    await page.locator('button[type=submit]').click();

    await expect(page).toHaveURL(/\/login\?error/);
    // Vẫn ở trang đăng nhập, không lọt vào trong
    await expect(page.locator('input[type=password]')).toBeVisible();
  });

  test('Đăng nhập học sinh rồi đăng xuất', async ({ page }) => {
    const problems = watchProblems(page);

    await login(page, 'student');
    expect(page.url(), 'đăng nhập xong không được ở lại /login').not.toContain('/login');
    await expect(page.locator('.sidebar')).toBeVisible();

    await logout(page);
    await expect(page).toHaveURL(/loggedOut|\/$/);

    expectNoProblems(problems, 'luồng đăng nhập/đăng xuất');
  });

  test('Đăng ký học sinh mới kèm mục tiêu, mục tiêu được lưu lại', async ({ page }) => {
    const email = uniqueEmail('hs');

    await page.goto('/register?goal=thi-thpt');
    // Mục tiêu từ landing phải được điền sẵn
    await expect(page.locator('select#goal')).toHaveValue('thi-thpt');

    await page.locator('input[name=fullName]').fill('E2E Hoc Sinh');
    await page.locator('input[name=email]').fill(email);
    await page.locator('input[name=password]').fill('e2e-Test-2026');
    await page.locator('input[name=grade][value="Lớp 11"]').check();
    await page.locator('button[type=submit]').click();

    // Đăng ký xong là tự đăng nhập luôn
    await page.waitForURL((u) => !u.pathname.startsWith('/register'));
    await expect(page.locator('.sidebar')).toBeVisible();

    // Mục tiêu phải xuất hiện ở trang quản trị
    await logout(page);
    await login(page, 'admin');
    await page.goto('/admin/users');
    const row = page.locator('.card', { hasText: email });
    await expect(row).toBeVisible();
    await expect(row).toContainText('Ôn thi tốt nghiệp THPT');
    await expect(row, 'người mới phải được tự cấp Premium').toContainText('Premium');
  });

  test('Không cho đăng ký trùng email', async ({ page }) => {
    await page.goto('/register');
    await page.locator('input[name=fullName]').fill('Trung Email');
    await page.locator('input[name=email]').fill(ACCOUNTS.student.email);
    await page.locator('input[name=password]').fill('e2e-Test-2026');
    await page.locator('input[name=grade][value="Lớp 10"]').check();
    await page.locator('button[type=submit]').click();

    await expect(page.locator('.err')).toContainText(/đã được đăng ký/i);
  });

  test('Thiếu khối lớp thì không đăng ký được', async ({ page }) => {
    await page.goto('/register');
    await page.locator('input[name=fullName]').fill('Thieu Khoi');
    await page.locator('input[name=email]').fill(uniqueEmail('nograde'));
    await page.locator('input[name=password]').fill('e2e-Test-2026');
    await page.locator('button[type=submit]').click();

    await expect(page.locator('.err')).toContainText(/khối lớp/i);
  });
});

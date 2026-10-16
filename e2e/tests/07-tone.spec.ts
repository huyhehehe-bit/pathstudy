import { test, expect } from '@playwright/test';
import { registerFreshStudent, expectNoHorizontalOverflow, attachShot, login, logout } from './helpers';

/**
 * Chọn giao diện: Sáng / Tối / Hồng.
 *
 * Ngữ cảnh TRẮNG (không nạp da-chon-tone.json như các file khác) để giả lập
 * đúng người dùng mới tinh — đó mới là người nhìn thấy hộp chọn.
 */
test.use({ storageState: { cookies: [], origins: [] } });

const doSang = (mau: string) => {
  const [r, g, b] = mau.match(/\d+/g)!.map(Number);
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};

test.describe('Giao diện Sáng / Tối / Hồng', () => {
  test('Người mới được hỏi chọn, chọn xong không hỏi lại', async ({ page }, testInfo) => {
    await page.goto('/');

    const hop = page.locator('.mode-gate');
    await expect(hop, 'người mới phải được hỏi chọn giao diện').toBeVisible();
    await attachShot(page, testInfo, 'hop-chon-giao-dien.png');

    await page.locator('.mode-opt[data-pick="sang"]').click();
    await expect(hop, 'chọn xong thì hộp phải biến mất').toHaveCount(0);
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'light');

    await page.reload();
    await expect(page.locator('.mode-gate')).toHaveCount(0);
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'light');
  });

  test('Học sinh có đủ 3 lựa chọn, trang quản trị chỉ có 2', async ({ page }) => {
    // Khách vãng lai cũng được tính là học sinh (GlobalModelAdvice.isStudent
    // trả true khi chưa đăng nhập) — trang chủ vốn đã trang trí kiểu Kawai.
    await page.goto('/');
    await expect(page.locator('.mode-opt')).toHaveCount(3);
    await expect(page.locator('.mode-opt[data-pick="hong"]')).toBeVisible();

    await page.locator('.mode-opt[data-pick="sang"]').click();
    await registerFreshStudent(page);
    await page.locator('#btnThemeSwitch').click();
    await expect(page.locator('.mode-opt')).toHaveCount(3);
    await page.locator('.mode-gate-huy').click(); // đóng hộp, nếu không nó che cả trang

    // Admin thì không: bản Hồng không hợp với trang quản trị.
    await logout(page);
    await login(page, 'admin');
    await page.locator('#btnThemeSwitch').click();
    await expect(page.locator('.mode-opt')).toHaveCount(2);
    await expect(page.locator('.mode-opt[data-pick="hong"]')).toHaveCount(0);
  });

  test('Ba template ra ba nền khác nhau, và nền sáng phải sáng thật', async ({ page }) => {
    await page.goto('/');
    await page.locator('.mode-opt[data-pick="sang"]').click();
    await registerFreshStudent(page);
    const nen = async () => page.evaluate(() => getComputedStyle(document.body).backgroundColor);

    const sang = await nen();

    await page.locator('#btnThemeSwitch').click();
    await page.locator('.mode-opt[data-pick="toi"]').click();
    const toi = await nen();

    await page.locator('#btnThemeSwitch').click();
    await page.locator('.mode-opt[data-pick="hong"]').click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'kawai');
    const hong = await nen();

    expect(new Set([sang, toi, hong]).size, 'ba template phải ra ba nền khác nhau').toBe(3);
    expect(doSang(sang), 'bản Sáng mà nền lại tối').toBeGreaterThan(180);
    expect(doSang(toi), 'bản Tối mà nền lại sáng').toBeLessThan(80);
  });

  test('Nút mặt trời/mặt trăng giữ nguyên phong cách, chỉ đổi ngày đêm', async ({ page }) => {
    await page.goto('/');
    await page.locator('.mode-opt[data-pick="sang"]').click();
    await registerFreshStudent(page);

    await page.locator('#btnThemeSwitch').click();
    await page.locator('.mode-opt[data-pick="hong"]').click();

    // Đang ở Hồng mà bấm mặt trăng thì phải ra Hồng ban đêm, không nhảy về pastel
    await page.locator('#btnModeSwitch').click();
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'dark');
    await expect(page.locator('html'), 'bấm ngày/đêm không được đổi phong cách')
      .toHaveAttribute('data-theme', 'kawai');

    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'dark');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'kawai');
  });

  test('Bấm "Để nguyên" thì không đổi gì', async ({ page }) => {
    await page.goto('/');
    await page.locator('.mode-opt[data-pick="toi"]').click();
    await registerFreshStudent(page);

    await page.locator('#btnThemeSwitch').click();
    await page.locator('.mode-opt[data-pick="hong"]').hover();
    await page.locator('.mode-gate-huy').click();

    await expect(page.locator('.mode-gate')).toHaveCount(0);
    await expect(page.locator('html'), 'xem thử rồi huỷ thì phải quay về cũ')
      .toHaveAttribute('data-theme', 'pastel');
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'dark');
  });

  test('Hộp chọn không tràn ngang ở cỡ điện thoại', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.mode-gate')).toBeVisible();
    await expectNoHorizontalOverflow(page, 'trang có hộp chọn giao diện');
  });
});

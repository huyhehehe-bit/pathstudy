import { test, expect } from '@playwright/test';
import { registerFreshStudent, ensureEnrolled, expectNoHorizontalOverflow, attachShot } from './helpers';

/** Trang học sinh hay vào nhất — phải vừa mọi cỡ màn. */
const STUDENT_PAGES = ['/path', '/progress', '/exams', '/library', '/feedback', '/subjects/add'];

test.describe('Responsive', () => {
  test.beforeEach(async ({ page }) => {
    await registerFreshStudent(page);
    await ensureEnrolled(page);
  });

  for (const path of STUDENT_PAGES) {
    test(`${path} không tràn ngang`, async ({ page }) => {
      await page.goto(path);
      await expectNoHorizontalOverflow(page, path);
    });
  }

  test('Mobile: sidebar thành bottom-bar, đủ 5 ô, không che nội dung', async ({ page }, testInfo) => {
    test.skip(test.info().project.name !== 'mobile', 'chỉ chạy ở cỡ điện thoại');

    await page.goto('/path');

    const sidebar = page.locator('.sidebar');
    const box = await sidebar.boundingBox();
    const vh = page.viewportSize()!.height;
    expect(box, 'phải thấy thanh điều hướng').not.toBeNull();
    expect(Math.abs((box!.y + box!.height) - vh),
      'thanh điều hướng phải nằm sát đáy màn hình').toBeLessThan(3);

    const visibleItems = page.locator('.nav-item:visible');
    const count = await visibleItems.count();
    expect(count, 'bottom-bar nên gọn trong 5 ô').toBeLessThanOrEqual(5);
    expect(count, 'phải có mục để bấm').toBeGreaterThan(2);

    // Nội dung phải chừa chỗ cho thanh dưới, không bị che
    const padBottom = await page.locator('.main').evaluate(
      (el) => parseInt(getComputedStyle(el).paddingBottom, 10),
    );
    expect(padBottom, 'main phải có padding-bottom tránh bị bottom-bar che').toBeGreaterThan(40);

    await attachShot(page, testInfo, 'mobile-bottom-nav.png');
  });

  test('Mobile: nút "Thêm" mở và đóng được tấm mục phụ', async ({ page }) => {
    test.skip(test.info().project.name !== 'mobile', 'chỉ chạy ở cỡ điện thoại');

    await page.goto('/path');
    const more = page.locator('.nav-more');
    await expect(more).toBeVisible();

    const before = await page.locator('.nav-item:visible').count();
    await more.click();

    await expect(page.locator('body')).toHaveClass(/nav-open/);
    await expect(more).toHaveAttribute('aria-expanded', 'true');
    const after = await page.locator('.nav-item:visible').count();
    expect(after, 'mở tấm "Thêm" phải lộ thêm mục').toBeGreaterThan(before);
    await expect(page.locator('.sidebar-foot')).toBeVisible();

    await more.click();
    await expect(page.locator('body')).not.toHaveClass(/nav-open/);
    await expect(more).toHaveAttribute('aria-expanded', 'false');
  });

  test('Desktop: sidebar thu gọn, rê chuột vào thì mở rộng', async ({ page }) => {
    test.skip(test.info().project.name !== 'desktop', 'chỉ chạy ở cỡ desktop');

    await page.goto('/path');
    const sidebar = page.locator('.sidebar');

    const collapsed = (await sidebar.boundingBox())!.width;
    expect(collapsed, 'mặc định phải là cột icon hẹp').toBeLessThan(120);

    // Lỗi từng gặp: sidebar position:fixed làm .main rơi vào cột 1 chỉ 76px
    const mainLeft = (await page.locator('.main').boundingBox())!.x;
    expect(mainLeft, 'nội dung không được nằm dưới thanh rail').toBeGreaterThanOrEqual(collapsed - 1);

    await sidebar.hover();
    await expect(async () => {
      const w = (await sidebar.boundingBox())!.width;
      expect(w, 'rê chuột vào thì sidebar phải rộng ra').toBeGreaterThan(collapsed + 50);
    }).toPass({ timeout: 3000 });
  });

  test('Ô nhập đủ 16px ở mobile để iOS không tự phóng to', async ({ page }) => {
    test.skip(test.info().project.name !== 'mobile', 'chỉ chạy ở cỡ điện thoại');

    await page.goto('/feedback');
    const fields = page.locator('input:visible, select:visible, textarea:visible');
    const n = await fields.count();
    test.skip(n === 0, 'trang này không có ô nhập');

    for (let i = 0; i < n; i++) {
      const el = fields.nth(i);
      const type = await el.getAttribute('type');
      if (type && ['radio', 'checkbox', 'hidden', 'submit', 'button'].includes(type)) continue;
      const size = await el.evaluate((e) => parseFloat(getComputedStyle(e).fontSize));
      expect(size, `ô nhập thứ ${i} có cỡ chữ ${size}px, dưới 16px sẽ khiến iOS phóng to`)
        .toBeGreaterThanOrEqual(16);
    }
  });
});

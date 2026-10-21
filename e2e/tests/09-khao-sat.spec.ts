import { test, expect } from '@playwright/test';
import { registerFreshStudent, completePlacement } from './helpers';

/**
 * Lời mời khảo sát trải nghiệm.
 *
 * Ngưỡng thật là 5 phút nên test không chờ thật — nó ghi thẳng mốc thời gian
 * "đã dùng web từ 6 phút trước" vào localStorage rồi tải lại trang, đúng cách
 * mã nguồn đọc dữ liệu đó.
 */
const SAU_6_PHUT = 6 * 60 * 1000;

async function daDungWebLau(page: import('@playwright/test').Page) {
  await page.evaluate((lui) => {
    localStorage.setItem('pathstudy-khaosat-batdau', String(Date.now() - lui));
    localStorage.removeItem('pathstudy-khaosat-xong');
  }, SAU_6_PHUT);
}

test.describe('Lời mời khảo sát', () => {
  test('Dùng web đủ lâu thì hiện lời mời, có link tới form', async ({ page }) => {
    await registerFreshStudent(page);
    await daDungWebLau(page);
    await page.reload();

    const moi = page.locator('.ks-moi');
    await expect(moi, 'dùng đủ 5 phút thì phải hiện lời mời').toBeVisible();
    await expect(moi.locator('.ks-mo')).toHaveAttribute('href', /forms\.gle|docs\.google\.com/);
    await expect(moi.locator('.ks-mo')).toHaveAttribute('target', '_blank');
  });

  test('ĐANG LÀM BÀI thì tuyệt đối không hiện', async ({ page }) => {
    await registerFreshStudent(page);
    await daDungWebLau(page);

    await page.goto('/placement/toan/test');
    await expect(page.locator('.q-card').first(), 'phải đang ở trang làm bài').toBeVisible();
    // chờ qua thời điểm đáng lẽ hiện, để chắc chắn không phải nó hiện chậm
    await page.waitForTimeout(1500);
    await expect(page.locator('.ks-moi'), 'đang làm bài mà vẫn mời là cắt ngang bài làm')
      .toHaveCount(0);
  });

  test('Làm bài xong, sang trang khác thì mới mời', async ({ page }) => {
    await registerFreshStudent(page);
    await daDungWebLau(page);
    await completePlacement(page, 'toan');

    await page.goto('/path');
    await expect(page.locator('.ks-moi'), 'rời trang làm bài thì mới được mời').toBeVisible();
  });

  test('Bấm "Để sau" thì không mời lại nữa', async ({ page }) => {
    await registerFreshStudent(page);
    await daDungWebLau(page);
    await page.reload();

    await page.locator('.ks-moi .ks-thoi').click();
    await expect(page.locator('.ks-moi')).toHaveCount(0);

    await page.reload();
    await expect(page.locator('.ks-moi'), 'đã từ chối thì đừng hỏi lại').toHaveCount(0);
  });

  test('Người mới vào chưa đủ giờ thì chưa mời', async ({ page }) => {
    await registerFreshStudent(page);
    await page.evaluate(() => {
      localStorage.removeItem('pathstudy-khaosat-batdau');
      localStorage.removeItem('pathstudy-khaosat-xong');
    });
    await page.reload();
    await expect(page.locator('.ks-moi'), 'mới vào đã mời là làm phiền').toHaveCount(0);
  });
});

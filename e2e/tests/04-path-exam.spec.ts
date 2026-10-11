import { test, expect } from '@playwright/test';
import { registerFreshStudent, ensureEnrolled, completePlacement, watchProblems, expectNoProblems, expectNoHorizontalOverflow, attachShot } from './helpers';

test.describe('Lộ trình & bài học', () => {
  test.beforeEach(async ({ page }) => {
    await registerFreshStudent(page);
    await ensureEnrolled(page);
  });

  test('Mục lục lộ trình hiện đủ chương, mỗi chương đều có tiêu đề', async ({ page }, testInfo) => {
    const problems = watchProblems(page);
    await page.goto('/path');

    const chapters = page.locator('.toc-ch');
    const n = await chapters.count();
    expect(n, 'phải có chương').toBeGreaterThan(0);

    // Lỗi từng gặp: điều kiện Thymeleaf sai làm chương bị khoá mất tiêu đề
    const heads = page.locator('.toc-head');
    expect(await heads.count(), 'mọi chương đều phải có dòng tiêu đề').toBe(n);

    await expectNoHorizontalOverflow(page, '/path');
    await attachShot(page, testInfo, 'muc-luc-lo-trinh.png');
    expectNoProblems(problems, '/path');
  });

  test('Đề luyện không bị lặp ở từng chương cùng khối', async ({ page }) => {
    await page.goto('/path?subject=toan');
    const examBlocks = page.locator('.toc-exams');
    const grades = page.locator('.toc-grade');
    const gradeCount = await grades.count();

    // Đề gắn theo KHỐI, nên nhiều nhất là 1 khối đề cho mỗi nhãn khối
    expect(await examBlocks.count(),
      'số khối đề luyện không được vượt số khối lớp').toBeLessThanOrEqual(gradeCount);
  });

  test('Mở được một chương đã mở khoá và bài học hiển thị nội dung', async ({ page }) => {
    const problems = watchProblems(page);

    // Lộ trình chỉ mở khoá chương đầu SAU khi chấm bài kiểm tra đầu vào.
    await completePlacement(page, 'van');
    await page.goto('/path?subject=van');

    const openable = page.locator('a.toc-head[href^="/module/"]');
    expect(await openable.count(),
      'làm xong bài kiểm tra đầu vào thì phải có ít nhất 1 chương mở khoá').toBeGreaterThan(0);

    await openable.first().click();
    await expect(page).toHaveURL(/\/module\/\d+/);

    await expect(page.locator('.lesson-hero')).toBeVisible();
    const sections = page.locator('.sec');
    expect(await sections.count(), 'bài học phải có phần nội dung').toBeGreaterThan(0);
    await expect(sections.first().locator('.sec-body')).not.toBeEmpty();

    await expectNoHorizontalOverflow(page, 'trang bài học');
    expectNoProblems(problems, 'trang bài học');
  });

  test('Đường dẫn module không tồn tại không làm vỡ app', async ({ page }) => {
    const res = await page.goto('/module/99999999');
    // Hoặc 404 gọn gàng, hoặc chuyển hướng về lộ trình — miễn là không 500
    expect(res?.status(), 'không được lỗi máy chủ').toBeLessThan(500);
  });
});

test.describe('Đề thi', () => {
  test.beforeEach(async ({ page }) => {
    await registerFreshStudent(page);
    await ensureEnrolled(page);
  });

  test('Trang danh sách đề mở được', async ({ page }) => {
    const problems = watchProblems(page);
    await page.goto('/exams');
    await expect(page.locator('.content')).toBeVisible();
    await expectNoHorizontalOverflow(page, '/exams');
    expectNoProblems(problems, '/exams');
  });

  test('Làm một đề và nhận được phần xem lại bài làm', async ({ page }, testInfo) => {
    await page.goto('/exams');
    const links = page.locator('a[href^="/exam/"]');
    test.skip((await links.count()) === 0, 'chưa có đề nào cho khối của tài khoản này');

    await links.first().click();
    await page.waitForURL(/\/exam\/\d+|\/upgrade/);

    // Đề premium sẽ đẩy sang /upgrade — bỏ qua, không phải lỗi.
    // Phải kiểm tra TRƯỚC khi khẳng định URL, nếu không test sẽ trượt
    // ngay tại dòng assert rồi không bao giờ chạy tới lệnh skip.
    test.skip(page.url().includes('/upgrade'), 'đề này cần Premium');
    await expect(page).toHaveURL(/\/exam\/\d+/);

    const cards = page.locator('.q-card');
    const total = await cards.count();
    test.skip(total === 0, 'đề rỗng, chưa có câu hỏi');

    for (let i = 0; i < total; i++) {
      const radios = cards.nth(i).locator('input[type=radio]');
      const n = await radios.count();
      if (n > 0) await radios.nth(i % n).check();
    }
    await page.locator('form:has(.q-card) button[type=submit]').first().click();
    await page.waitForURL(/\/result/);

    await expect(page.locator('.ring-value')).toContainText('%');
    await expect(page.locator('.rv-item')).toHaveCount(total);

    await attachShot(page, testInfo, 'ket-qua-de-thi.png');
  });
});

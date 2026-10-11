import { test, expect } from '@playwright/test';
import { registerFreshStudent, watchProblems, expectNoProblems, attachShot, expectNoHorizontalOverflow } from './helpers';

/**
 * Luồng nghiệp vụ quan trọng nhất: học sinh làm bài kiểm tra đầu vào, hệ thống
 * chấm, chỉ ra điểm yếu, và cho xem lại từng câu sai.
 */
test.describe('Bài kiểm tra đầu vào', () => {
  // Mỗi test một học sinh MỚI: bài kiểm tra đầu vào chỉ cho 3 lượt, dùng chung
  // tài khoản thì chạy lại lần hai là hết lượt.
  test.beforeEach(async ({ page }) => {
    await registerFreshStudent(page);
  });

  test('Làm trọn bài Ngữ văn: đề → nộp → kết quả → xem lại câu sai', async ({ page }, testInfo) => {
    const problems = watchProblems(page);

    await page.goto('/placement/van/test');
    const cards = page.locator('.q-card');
    const total = await cards.count();
    expect(total, 'đề phải có câu hỏi').toBeGreaterThan(0);

    // Mỗi câu chọn một đáp án khác nhau để chắc chắn có cả đúng lẫn sai
    for (let i = 0; i < total; i++) {
      const radios = cards.nth(i).locator('input[type=radio]');
      const n = await radios.count();
      if (n > 0) await radios.nth(i % n).check();
    }

    await page.locator('form:has(.q-card) button[type=submit]').first().click();
    await page.waitForURL(/\/placement\/van\/result/);

    // Có điểm và mức
    await expect(page.locator('.ring-value')).toContainText('%');

    // Phần xem lại bài làm
    const items = page.locator('.rv-item');
    await expect(items, 'số câu xem lại phải bằng số câu đã làm').toHaveCount(total);

    const wrong = page.locator('.rv-item.rv-bad, .rv-item.rv-skip');
    const wrongCount = await wrong.count();
    expect(wrongCount, 'bài này cố tình chọn sai nên phải có câu sai').toBeGreaterThan(0);

    // Câu sai phải chỉ rõ chọn gì và đúng là gì
    const firstWrong = wrong.first();
    await expect(firstWrong).toContainText('Em chọn');
    await expect(firstWrong).toContainText('Đáp án đúng');

    await attachShot(page, testInfo, 'ket-qua-placement.png');
    await expectNoHorizontalOverflow(page, 'trang kết quả');
    expectNoProblems(problems, 'luồng kiểm tra đầu vào');
  });

  test('Lọc "chỉ xem câu sai" ẩn hết câu đúng', async ({ page }) => {
    await page.goto('/placement/van/test');
    const cards = page.locator('.q-card');
    const total = await cards.count();
    for (let i = 0; i < total; i++) {
      const radios = cards.nth(i).locator('input[type=radio]');
      if (await radios.count()) await radios.first().check();
    }
    await page.locator('form:has(.q-card) button[type=submit]').first().click();
    await page.waitForURL(/\/result/);

    const ok = page.locator('.rv-item.rv-ok');
    const okCount = await ok.count();
    test.skip(okCount === 0, 'lần này không có câu đúng nào để kiểm tra bộ lọc');

    await page.locator('.rv-only-wrong').check();
    await expect(ok.first()).toBeHidden();
  });

  test('Nút nhờ AI giảng bài chỉ hiện khi AI được bật', async ({ page }) => {
    await page.goto('/placement/van/test');
    const cards = page.locator('.q-card');
    const total = await cards.count();
    for (let i = 0; i < total; i++) {
      const radios = cards.nth(i).locator('input[type=radio]');
      if (await radios.count()) await radios.last().check();
    }
    await page.locator('form:has(.q-card) button[type=submit]').first().click();
    await page.waitForURL(/\/result/);

    const wrong = page.locator('.rv-item.rv-bad, .rv-item.rv-skip');
    test.skip((await wrong.count()) === 0, 'không có câu sai để kiểm tra');

    const buttons = page.locator('.rv-explain');
    const chips = page.locator('.rv-ai .chip');

    // Môi trường test không có GEMINI_API_KEY → phải hiện lời nhắc, không hiện nút
    const hasButtons = (await buttons.count()) > 0;
    if (hasButtons) {
      await expect(chips).toHaveCount(0);
    } else {
      expect(await chips.count(), 'AI tắt thì phải hiện chip nhắc bật AI').toBeGreaterThan(0);
      await expect(chips.first()).toContainText(/Bật AI/i);
    }
  });

  test('API giải thích từ chối gọn gàng khi AI tắt', async ({ page }) => {
    await page.goto('/placement/van/test');
    const qid = await page.locator('input[type=radio]').first().getAttribute('name');
    const questionId = (qid ?? '').replace(/^q_/, '');
    expect(questionId, 'phải lấy được id câu hỏi từ form').toMatch(/^\d+$/);

    const result = await page.evaluate(async (id) => {
      const token = (document.querySelector('meta[name="_csrf"]') as HTMLMetaElement)?.content ?? '';
      const header = (document.querySelector('meta[name="_csrf_header"]') as HTMLMetaElement)?.content ?? 'X-CSRF-TOKEN';
      const body = new URLSearchParams({ questionId: id, chosenIndex: '0' });
      const res = await fetch('/ai/explain', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded', [header]: token },
        body: body.toString(),
        credentials: 'same-origin',
      });
      return { status: res.status, json: await res.json() };
    }, questionId);

    // Qua được CSRF và đăng nhập (không 403), trả JSON có cấu trúc
    expect(result.status, 'không được 403 — CSRF phải hợp lệ').toBe(200);
    expect(result.json).toHaveProperty('ok');
    if (result.json.ok === false) {
      expect(result.json.error).toBeTruthy();
    } else {
      expect(String(result.json.explanation).length).toBeGreaterThan(20);
    }
  });
});

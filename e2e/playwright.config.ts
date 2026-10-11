import { defineConfig, devices } from '@playwright/test';
import path from 'node:path';
import fs from 'node:fs';

const repoRoot = path.resolve(__dirname, '..');

/**
 * JDK nằm trong .tools của dự án (không commit). Dùng đường dẫn TUYỆT ĐỐI —
 * cmd trên Windows không chạy được lệnh kiểu ".tools/jdk/.../java".
 * Máy nào có java sẵn trong PATH thì dùng luôn.
 */
const bundledJava = path.join(
  repoRoot, '.tools', 'jdk', 'jdk-21.0.12.1+1', 'bin',
  process.platform === 'win32' ? 'java.exe' : 'java',
);
const javaBin = fs.existsSync(bundledJava) ? bundledJava : 'java';
const appJar = path.join(repoRoot, 'target', 'pathstudy.jar');

if (!fs.existsSync(appJar)) {
  throw new Error(
    `Chưa có ${appJar}. Hãy build trước:\n` +
    `  mvn -DskipTests package`,
  );
}

/**
 * E2E cho PathStudy.
 *
 * Playwright tự khởi động app trước khi chạy test (webServer bên dưới), nên
 * chỉ cần `npm test` là đủ — không phải bật server bằng tay.
 *
 * AN TOÀN DỮ LIỆU:
 * - Chạy ở cổng 8099, KHÔNG phải 8080/8090 của môi trường dev.
 * - Dùng file H2 riêng `data/pathstudy-e2e`, không đụng `data/pathstudy`.
 * - TUYỆT ĐỐI không trỏ vào Render/production.
 */
const PORT = 8099;
const BASE_URL = `http://127.0.0.1:${PORT}`;

export default defineConfig({
  testDir: './tests',
  // Chạy tuần tự: app dùng chung 1 DB H2, chạy song song sẽ đá nhau
  // (vd test này đăng nhập admin trong khi test kia đang là học sinh).
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },

  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],

  use: {
    baseURL: BASE_URL,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    locale: 'vi-VN',
  },

  projects: [
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 900 } },
    },
    {
      name: 'mobile',
      // 375x812 = cỡ iPhone X, mốc mà bottom-nav thay cho sidebar
      use: { ...devices['Pixel 5'], viewport: { width: 375, height: 812 } },
    },
    {
      name: 'tablet',
      use: { ...devices['Desktop Chrome'], viewport: { width: 768, height: 1024 } },
    },
  ],

  webServer: {
    command: [
      `"${javaBin}"`,
      '-jar', `"${appJar}"`,
      `--server.port=${PORT}`,
      '--spring.datasource.url=jdbc:h2:file:./data/pathstudy-e2e',
    ].join(' '),
    cwd: repoRoot,
    url: `${BASE_URL}/login`,
    reuseExistingServer: !process.env.CI,
    // Lần đầu phải seed toàn bộ nội dung (24 chương Toán, Văn, Anh) nên lâu.
    timeout: 240_000,
    stdout: 'pipe',
    stderr: 'pipe',
  },
});

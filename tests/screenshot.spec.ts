import { test } from '@playwright/test';

test('take screenshot of puzzle app', async ({ page }) => {
  await page.goto('http://localhost:5173');

  // 페이지 로딩 대기
  await page.waitForTimeout(1000);

  // 새 퍼즐 생성 버튼 클릭
  await page.getByRole('button', { name: '새 퍼즐 생성' }).click();

  // 퍼즐 로딩 대기
  await page.waitForTimeout(3000);

  // 스크린샷 저장
  await page.screenshot({ path: 'puzzle-generated.png', fullPage: true });
});

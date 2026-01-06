import { test, expect } from '@playwright/test';

test.describe('한글 복합 모음 입력 테스트', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:5173');

    // 퍼즐 생성
    await page.click('button:has-text("새 퍼즐 생성")');
    await page.waitForSelector('.puzzle-grid');
  });

  test('의 (ㅇ+ㅡ+ㅣ) 입력', async ({ page }) => {
    // 첫 번째 활성 셀 찾기
    const firstInput = page.locator('.puzzle-cell.active input').first();
    await firstInput.click();

    // "의" 입력 (복합 모음)
    await firstInput.pressSequentially('의', { delay: 100 });

    // 입력값 확인
    await expect(firstInput).toHaveValue('의');
  });

  test('왜 (ㅇ+ㅗ+ㅐ) 입력', async ({ page }) => {
    const firstInput = page.locator('.puzzle-cell.active input').first();
    await firstInput.click();

    await firstInput.pressSequentially('왜', { delay: 100 });

    await expect(firstInput).toHaveValue('왜');
  });

  test('귀 (ㄱ+ㅜ+ㅣ) 입력', async ({ page }) => {
    const firstInput = page.locator('.puzzle-cell.active input').first();
    await firstInput.click();

    await firstInput.pressSequentially('귀', { delay: 100 });

    await expect(firstInput).toHaveValue('귀');
  });

  test('쥐 (ㅈ+ㅜ+ㅣ) 입력', async ({ page }) => {
    const firstInput = page.locator('.puzzle-cell.active input').first();
    await firstInput.click();

    await firstInput.pressSequentially('쥐', { delay: 100 });

    await expect(firstInput).toHaveValue('쥐');
  });

  test('뷔 (ㅂ+ㅜ+ㅣ) 입력', async ({ page }) => {
    const firstInput = page.locator('.puzzle-cell.active input').first();
    await firstInput.click();

    await firstInput.pressSequentially('뷔', { delay: 100 });

    await expect(firstInput).toHaveValue('뷔');
  });
});

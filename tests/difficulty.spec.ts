import { test, expect } from '@playwright/test';

test.describe('Difficulty Level Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('default difficulty is "전체"', async ({ page }) => {
    const levelSelect = page.locator('select');
    await expect(levelSelect).toHaveValue('');
  });

  test('can select 초급 difficulty', async ({ page }) => {
    const levelSelect = page.locator('select');
    await levelSelect.selectOption('초급');
    await expect(levelSelect).toHaveValue('초급');
  });

  test('can select 중급 difficulty', async ({ page }) => {
    const levelSelect = page.locator('select');
    await levelSelect.selectOption('중급');
    await expect(levelSelect).toHaveValue('중급');
  });

  test('can select 고급 difficulty', async ({ page }) => {
    const levelSelect = page.locator('select');
    await levelSelect.selectOption('고급');
    await expect(levelSelect).toHaveValue('고급');
  });

  test('generates puzzle with 초급 difficulty', async ({ page }) => {
    // 난이도 선택
    const levelSelect = page.locator('select');
    await levelSelect.selectOption('초급');

    // 퍼즐 생성
    await page.getByRole('button', { name: '새 퍼즐 생성' }).click();

    // 퍼즐 그리드가 표시되는지 확인
    await expect(page.locator('.puzzle-grid')).toBeVisible({ timeout: 10000 });

    // 힌트 패널 확인
    await expect(page.getByText('가로 열쇠')).toBeVisible();
    await expect(page.getByText('세로 열쇠')).toBeVisible();
  });

  test('generates puzzle with 중급 difficulty', async ({ page }) => {
    const levelSelect = page.locator('select');
    await levelSelect.selectOption('중급');

    await page.getByRole('button', { name: '새 퍼즐 생성' }).click();

    await expect(page.locator('.puzzle-grid')).toBeVisible({ timeout: 10000 });
  });

  test('generates puzzle with 고급 difficulty', async ({ page }) => {
    const levelSelect = page.locator('select');
    await levelSelect.selectOption('고급');

    await page.getByRole('button', { name: '새 퍼즐 생성' }).click();

    await expect(page.locator('.puzzle-grid')).toBeVisible({ timeout: 10000 });
  });

  test('puzzle info displays correctly', async ({ page }) => {
    await page.getByRole('button', { name: '새 퍼즐 생성' }).click();

    // 퍼즐 정보가 표시되는지 확인
    const puzzleInfo = page.locator('.puzzle-info');
    await expect(puzzleInfo).toBeVisible({ timeout: 10000 });

    // "총 X개 단어" 형식 확인
    await expect(puzzleInfo).toContainText('총');
    await expect(puzzleInfo).toContainText('개 단어');
    await expect(puzzleInfo).toContainText('그리드');
  });
});

test.describe('Word Count Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('default word count is 10', async ({ page }) => {
    const wordCountInput = page.locator('input[type="number"]');
    await expect(wordCountInput).toHaveValue('10');
  });

  test('can change word count', async ({ page }) => {
    const wordCountInput = page.locator('input[type="number"]');
    await wordCountInput.fill('5');
    await expect(wordCountInput).toHaveValue('5');
  });

  test('word count has min value 3', async ({ page }) => {
    const wordCountInput = page.locator('input[type="number"]');
    await expect(wordCountInput).toHaveAttribute('min', '3');
  });

  test('word count has max value 50', async ({ page }) => {
    const wordCountInput = page.locator('input[type="number"]');
    await expect(wordCountInput).toHaveAttribute('max', '50');
  });

  test('generates puzzle with custom word count', async ({ page }) => {
    const wordCountInput = page.locator('input[type="number"]');
    await wordCountInput.fill('5');

    await page.getByRole('button', { name: '새 퍼즐 생성' }).click();

    await expect(page.locator('.puzzle-grid')).toBeVisible({ timeout: 10000 });
  });
});

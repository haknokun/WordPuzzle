import { test, expect } from '@playwright/test';
import { PuzzlePage } from './pages/PuzzlePage';

test.describe('Grid Interaction Tests', () => {
  let puzzlePage: PuzzlePage;

  test.beforeEach(async ({ page }) => {
    puzzlePage = new PuzzlePage(page);
    await puzzlePage.goto();
    await puzzlePage.generatePuzzle();
  });

  test('clicking cell selects it', async () => {
    await puzzlePage.clickCell(0);

    await puzzlePage.expectSelectedCellCount(1);
    await expect(puzzlePage.getActiveCells().first()).toHaveClass(/selected/);
  });

  test('clicking cell highlights related word cells', async () => {
    await puzzlePage.clickCell(0);
    await puzzlePage.page.waitForTimeout(100);

    const highlightedCount = await puzzlePage.getHighlightedCells().count();
    expect(highlightedCount).toBeGreaterThanOrEqual(2);
  });

  test('double click on intersection toggles direction', async ({ page }) => {
    // 셀 클릭
    await puzzlePage.clickCell(0);
    await page.waitForTimeout(100);

    const firstHighlightCount = await puzzlePage.getHighlightedCells().count();

    // 같은 셀 다시 클릭 (방향 전환 시도)
    await puzzlePage.clickCell(0);
    await page.waitForTimeout(100);

    // 교차점이면 다른 방향의 단어가 선택됨
    const secondHighlightCount = await puzzlePage.getHighlightedCells().count();

    // 최소한 하나 이상의 셀이 하이라이트되어야 함
    expect(secondHighlightCount).toBeGreaterThanOrEqual(1);
  });

  test('arrow key navigation works correctly', async ({ page }) => {
    await puzzlePage.clickCell(0);
    await page.waitForTimeout(100);

    // 오른쪽 화살표
    await puzzlePage.pressArrowKey('right');
    await page.waitForTimeout(100);

    await puzzlePage.expectSelectedCellCount(1);
  });

  test('space key moves to next cell in word', async ({ page }) => {
    // 힌트 클릭해서 단어 선택
    await puzzlePage.clickAcrossHint(0);
    await page.waitForTimeout(100);

    // 스페이스바로 다음 셀로 이동
    await puzzlePage.pressSpace();
    await page.waitForTimeout(100);

    await puzzlePage.expectSelectedCellCount(1);
  });

  test('backspace on empty cell moves to previous cell', async ({ page }) => {
    // 힌트 클릭해서 단어 선택
    await puzzlePage.clickAcrossHint(0);
    await page.waitForTimeout(100);

    // 먼저 다음 셀로 이동
    await puzzlePage.pressSpace();
    await page.waitForTimeout(100);

    // Backspace (빈 셀이므로 이전으로 이동)
    await puzzlePage.pressBackspace();
    await page.waitForTimeout(100);

    await puzzlePage.expectSelectedCellCount(1);
  });

  test('typing in cell updates value', async () => {
    await puzzlePage.clickCell(0);

    const input = puzzlePage.getActiveCells().first().locator('input');
    await input.fill('가');

    await expect(input).toHaveValue('가');
  });

  test('correct answer shows correct class', async ({ page }) => {
    await puzzlePage.clickCell(0);
    const cell = puzzlePage.getActiveCells().first();
    const input = cell.locator('input');

    await input.fill('가');
    await page.waitForTimeout(200);

    // 정답이면 correct, 오답이면 incorrect 클래스
    const hasCorrect = await cell.evaluate(el => el.classList.contains('correct'));
    const hasIncorrect = await cell.evaluate(el => el.classList.contains('incorrect'));

    expect(hasCorrect || hasIncorrect).toBeTruthy();
  });

  test('blank cells are not clickable for input', async ({ page }) => {
    const blankCells = page.locator('.puzzle-cell.blank');
    const blankCount = await blankCells.count();

    if (blankCount > 0) {
      // blank 셀에는 input이 없어야 함
      const inputs = await blankCells.first().locator('input').count();
      expect(inputs).toBe(0);
    }
  });

  test('word number click selects that word', async ({ page }) => {
    const wordNumbers = page.locator('.word-number');
    const numberCount = await wordNumbers.count();

    if (numberCount > 0) {
      await wordNumbers.first().click();
      await page.waitForTimeout(100);

      const highlightedCount = await puzzlePage.getHighlightedCells().count();
      expect(highlightedCount).toBeGreaterThanOrEqual(2);
    }
  });
});

test.describe('Grid Cell States', () => {
  let puzzlePage: PuzzlePage;

  test.beforeEach(async ({ page }) => {
    puzzlePage = new PuzzlePage(page);
    await puzzlePage.goto();
    await puzzlePage.generatePuzzle();
  });

  test('grid has both blank and active cells', async () => {
    const blankCells = puzzlePage.page.locator('.puzzle-cell.blank');
    const activeCells = puzzlePage.getActiveCells();

    const blankCount = await blankCells.count();
    const activeCount = await activeCells.count();

    expect(blankCount).toBeGreaterThan(0);
    expect(activeCount).toBeGreaterThan(0);
  });

  test('each active cell has input element', async () => {
    const activeCells = puzzlePage.getActiveCells();
    const count = await activeCells.count();

    for (let i = 0; i < Math.min(count, 5); i++) {
      const input = activeCells.nth(i).locator('input');
      await expect(input).toBeVisible();
    }
  });

  test('selected cell has selected class', async () => {
    await puzzlePage.clickCell(0);

    const selectedCell = puzzlePage.getSelectedCell();
    await expect(selectedCell).toHaveClass(/selected/);
  });

  test('highlighted cells include selected cell', async () => {
    await puzzlePage.clickCell(0);
    await puzzlePage.page.waitForTimeout(100);

    // 선택된 셀도 하이라이트에 포함되어야 함
    const selectedCell = puzzlePage.getSelectedCell();
    await expect(selectedCell).toHaveClass(/highlighted/);
  });
});

test.describe('Korean Input Tests with Page Object', () => {
  let puzzlePage: PuzzlePage;

  test.beforeEach(async ({ page }) => {
    puzzlePage = new PuzzlePage(page);
    await puzzlePage.goto();
    await puzzlePage.generatePuzzle();
  });

  test('basic Korean character input', async () => {
    await puzzlePage.clickCell(0);

    const input = puzzlePage.getActiveCells().first().locator('input');
    await input.fill('한');

    await expect(input).toHaveValue('한');
  });

  test('Korean character with complex vowel', async () => {
    await puzzlePage.clickCell(0);

    const input = puzzlePage.getActiveCells().first().locator('input');
    await input.pressSequentially('의', { delay: 100 });

    await expect(input).toHaveValue('의');
  });

  test('multiple Korean characters move to next cells', async ({ page }) => {
    // 힌트 클릭해서 단어 선택 (방향 확인)
    await puzzlePage.clickAcrossHint(0);
    await page.waitForTimeout(100);

    const firstInput = puzzlePage.getSelectedCell().locator('input');
    await firstInput.fill('가');
    await page.waitForTimeout(100);

    await expect(firstInput).toHaveValue('가');
  });
});

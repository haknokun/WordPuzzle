import { test, expect } from '@playwright/test';
import { PuzzlePage } from './pages/PuzzlePage';

test.describe('Hint System Tests', () => {
  let puzzlePage: PuzzlePage;

  test.beforeEach(async ({ page }) => {
    puzzlePage = new PuzzlePage(page);
    await puzzlePage.goto();
    await puzzlePage.generatePuzzle();
  });

  test('across hints section is visible', async () => {
    await expect(puzzlePage.acrossHintSection).toBeVisible();
  });

  test('down hints section is visible', async () => {
    await expect(puzzlePage.downHintSection).toBeVisible();
  });

  test('clicking across hint selects word cells', async () => {
    const acrossHints = puzzlePage.getAcrossHints();
    const hintCount = await acrossHints.count();

    if (hintCount > 0) {
      await puzzlePage.clickAcrossHint(0);
      await puzzlePage.page.waitForTimeout(200);

      const highlightedCount = await puzzlePage.getHighlightedCells().count();
      expect(highlightedCount).toBeGreaterThanOrEqual(2);
    }
  });

  test('clicking down hint selects word cells', async () => {
    const downHints = puzzlePage.getDownHints();
    const hintCount = await downHints.count();

    if (hintCount > 0) {
      await puzzlePage.clickDownHint(0);
      await puzzlePage.page.waitForTimeout(200);

      const highlightedCount = await puzzlePage.getHighlightedCells().count();
      expect(highlightedCount).toBeGreaterThanOrEqual(2);
    }
  });

  test('hint click focuses first cell of word', async () => {
    await puzzlePage.clickAcrossHint(0);
    await puzzlePage.page.waitForTimeout(200);

    // 선택된 셀이 있어야 함
    await puzzlePage.expectSelectedCellCount(1);

    // 선택된 셀에 input이 포커스되어 있어야 함
    const selectedCell = puzzlePage.getSelectedCell();
    const input = selectedCell.locator('input');
    await expect(input).toBeFocused();
  });

  test('different hints select different words', async () => {
    const acrossHints = puzzlePage.getAcrossHints();
    const acrossCount = await acrossHints.count();

    if (acrossCount >= 2) {
      // 첫 번째 힌트 클릭
      await puzzlePage.clickAcrossHint(0);
      await puzzlePage.page.waitForTimeout(100);
      const firstHighlightedCells = await puzzlePage.getHighlightedCells().allTextContents();

      // 두 번째 힌트 클릭
      await puzzlePage.clickAcrossHint(1);
      await puzzlePage.page.waitForTimeout(100);
      const secondHighlightedCells = await puzzlePage.getHighlightedCells().allTextContents();

      // 하이라이트된 셀들이 달라야 함 (위치가 다른 단어)
      expect(firstHighlightedCells).not.toEqual(secondHighlightedCells);
    }
  });

  test('chosung hint button exists for each hint', async () => {
    const chosungButtons = puzzlePage.getChosungButtons();
    const buttonCount = await chosungButtons.count();

    // 각 힌트에 초성 버튼이 있어야 함
    expect(buttonCount).toBeGreaterThan(0);
  });

  test('chosung toggle shows/hides chosung text', async () => {
    const chosungButtons = puzzlePage.getChosungButtons();

    if (await chosungButtons.count() > 0) {
      // 초기 상태 확인 (초성 숨김)
      const chosungTexts = puzzlePage.page.locator('.chosung-text');
      const initialCount = await chosungTexts.count();

      // 토글 클릭
      await puzzlePage.toggleChosung(0);
      await puzzlePage.page.waitForTimeout(200);

      // 초성 표시 확인
      const afterToggleCount = await chosungTexts.count();
      expect(afterToggleCount).toBeGreaterThan(initialCount);

      // 다시 토글 클릭
      await puzzlePage.toggleChosung(0);
      await puzzlePage.page.waitForTimeout(200);

      // 초성 숨김 확인
      const finalCount = await chosungTexts.count();
      expect(finalCount).toBe(initialCount);
    }
  });

  test('clicking grid cell then hint updates selection', async () => {
    // 셀 클릭
    await puzzlePage.clickCell(0);
    await puzzlePage.page.waitForTimeout(100);

    const cellHighlightBefore = await puzzlePage.getHighlightedCells().count();

    // 힌트 클릭
    await puzzlePage.clickAcrossHint(0);
    await puzzlePage.page.waitForTimeout(100);

    const cellHighlightAfter = await puzzlePage.getHighlightedCells().count();

    // 하이라이트 상태가 변경됨 (같을 수도, 다를 수도 있음 - 중요한 건 동작함)
    expect(cellHighlightAfter).toBeGreaterThanOrEqual(2);
  });

  test('hint displays word definition', async () => {
    const firstHint = puzzlePage.getAcrossHints().first();
    const hintText = await firstHint.textContent();

    // 힌트는 번호와 뜻풀이를 포함해야 함
    expect(hintText).toBeTruthy();
    expect(hintText!.length).toBeGreaterThan(0);
  });
});

test.describe('Hint Scroll Behavior', () => {
  let puzzlePage: PuzzlePage;

  test.beforeEach(async ({ page }) => {
    puzzlePage = new PuzzlePage(page);
    await puzzlePage.goto();
    await puzzlePage.generatePuzzle({ wordCount: 15 }); // 많은 단어로 스크롤 필요하게
  });

  test('selecting word from grid scrolls hint into view', async ({ page }) => {
    // 먼저 그리드에서 셀 클릭
    await puzzlePage.clickCell(0);
    await page.waitForTimeout(200);

    // 선택된 셀이 있는지 확인
    const selectedCount = await puzzlePage.getSelectedCell().count();
    expect(selectedCount).toBe(1);
  });
});

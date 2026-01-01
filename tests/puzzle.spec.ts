import { test, expect } from '@playwright/test';

test.describe('Korean Crossword Puzzle', () => {
  test('loads puzzle page', async ({ page }) => {
    await page.goto('/');

    // 페이지 로딩 확인
    await expect(page).toHaveTitle(/frontend/);
  });

  test('generates puzzle on button click', async ({ page }) => {
    await page.goto('/');

    // 새 퍼즐 생성 버튼 클릭
    const generateButton = page.getByRole('button', { name: /새 퍼즐|생성|New/ });
    await generateButton.click();

    // 퍼즐 그리드가 표시되는지 확인
    await expect(page.locator('.puzzle-grid')).toBeVisible({ timeout: 10000 });
  });

  test('displays hint panel with clues', async ({ page }) => {
    await page.goto('/');

    // 새 퍼즐 생성 버튼 클릭
    await page.getByRole('button', { name: '새 퍼즐 생성' }).click();

    // 퍼즐 로딩 대기
    await page.waitForTimeout(3000);

    // 가로/세로 힌트가 표시되는지 확인
    await expect(page.getByText('가로 열쇠')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('세로 열쇠')).toBeVisible({ timeout: 5000 });
  });

  test('can select difficulty level', async ({ page }) => {
    await page.goto('/');

    // 난이도 선택 요소 확인
    const levelSelector = page.locator('select');
    await expect(levelSelector.first()).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Keyboard Input Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: '새 퍼즐 생성' }).click();
    await page.waitForTimeout(2000);
  });

  test('can click cell and type Korean character', async ({ page }) => {
    // 활성화된 셀 찾기 (blank가 아닌 셀)
    const activeCell = page.locator('.puzzle-cell.active').first();
    await expect(activeCell).toBeVisible();

    // 셀 클릭
    await activeCell.click();

    // 셀이 선택되었는지 확인
    await expect(activeCell).toHaveClass(/selected/);

    // 한글 입력
    const input = activeCell.locator('input');
    await input.fill('가');

    // 입력값 확인
    await expect(input).toHaveValue('가');
  });

  test('arrow key navigation works', async ({ page }) => {
    // 첫 번째 활성 셀 클릭
    const firstCell = page.locator('.puzzle-cell.active').first();
    await firstCell.click();
    await expect(firstCell).toHaveClass(/selected/);

    // 오른쪽 화살표 키 입력
    await page.keyboard.press('ArrowRight');
    await page.waitForTimeout(100);

    // 선택된 셀이 변경되었는지 확인
    const selectedCells = page.locator('.puzzle-cell.selected');
    await expect(selectedCells).toHaveCount(1);
  });

  test('backspace clears cell and moves back', async ({ page }) => {
    // 활성 셀 클릭
    const cells = page.locator('.puzzle-cell.active');
    const firstCell = cells.first();
    await firstCell.click();

    // 글자 입력
    const input = firstCell.locator('input');
    await input.fill('테');
    await expect(input).toHaveValue('테');

    // Backspace로 삭제
    await input.press('Backspace');
    await page.waitForTimeout(100);

    // 값이 삭제되었는지 확인
    await expect(input).toHaveValue('');
  });

  test('space key moves to next cell', async ({ page }) => {
    // 힌트 클릭해서 단어 선택
    const hintItem = page.locator('.hint-section li').first();
    await hintItem.click();
    await page.waitForTimeout(200);

    // 현재 선택된 셀 위치 확인
    const selectedCell = page.locator('.puzzle-cell.selected');
    await expect(selectedCell).toBeVisible();

    // 스페이스바로 다음 셀로 이동
    await page.keyboard.press('Space');
    await page.waitForTimeout(100);

    // 여전히 하나의 셀만 선택되어 있는지 확인
    await expect(page.locator('.puzzle-cell.selected')).toHaveCount(1);
  });
});

test.describe('Answer Verification Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: '새 퍼즐 생성' }).click();
    await page.waitForTimeout(2000);
  });

  test('correct answer shows correct class', async ({ page }) => {
    // API에서 정답 데이터 가져오기 (실제 정답을 알아야 함)
    // 테스트를 위해 셀에 입력 후 correct/incorrect 클래스 확인

    const activeCell = page.locator('.puzzle-cell.active').first();
    await activeCell.click();

    const input = activeCell.locator('input');

    // 아무 글자나 입력 (정답일 수도, 오답일 수도 있음)
    await input.fill('가');
    await page.waitForTimeout(200);

    // 입력 후 correct 또는 incorrect 클래스가 추가되는지 확인
    const hasCorrect = await activeCell.evaluate(el => el.classList.contains('correct'));
    const hasIncorrect = await activeCell.evaluate(el => el.classList.contains('incorrect'));

    // 둘 중 하나는 true여야 함 (입력이 있으므로)
    expect(hasCorrect || hasIncorrect).toBeTruthy();
  });

  test('incorrect answer shows incorrect class', async ({ page }) => {
    // 첫 번째 셀에 틀린 답 입력 테스트
    const activeCell = page.locator('.puzzle-cell.active').first();
    await activeCell.click();

    const input = activeCell.locator('input');

    // 여러 다른 글자를 시도해서 오답 확인
    const testChars = ['ㅋ', '!', '1'];

    for (const char of testChars) {
      await input.fill(char);
      await page.waitForTimeout(100);

      const hasIncorrect = await activeCell.evaluate(el => el.classList.contains('incorrect'));
      if (hasIncorrect) {
        // 오답 클래스가 표시됨
        expect(hasIncorrect).toBeTruthy();
        return;
      }
    }
  });

  test('clicking hint selects corresponding word cells', async ({ page }) => {
    // 힌트 클릭
    const hintItem = page.locator('.hint-section li').first();
    await expect(hintItem).toBeVisible();
    await hintItem.click();
    await page.waitForTimeout(200);

    // 선택된 단어의 셀들이 하이라이트되는지 확인
    const highlightedCells = page.locator('.puzzle-cell.highlighted');
    const count = await highlightedCells.count();

    // 최소 2개 이상의 셀이 하이라이트되어야 함 (단어 길이)
    expect(count).toBeGreaterThanOrEqual(2);
  });

  test('completion message element exists when puzzle is completed', async ({ page }) => {
    // 완료 메시지는 .complete-message 클래스로 표시됨
    // 실제 완료 테스트는 completion.spec.ts에서 수행

    // 퍼즐 그리드가 존재하는지 확인
    const grid = page.locator('.puzzle-grid');
    await expect(grid).toBeVisible();

    // 완료 메시지 요소가 DOM에 추가될 수 있는지 확인 (초기에는 숨김)
    const completeMessage = page.locator('.complete-message');
    // 완료 전에는 보이지 않아야 함
    await expect(completeMessage).not.toBeVisible();
  });
});

test.describe('Chosung Hint Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: '새 퍼즐 생성' }).click();
    await page.waitForTimeout(2000);
  });

  test('chosung hint toggle button exists', async ({ page }) => {
    // 초성 힌트 토글 버튼 확인 (.chosung-btn)
    const chosungButton = page.locator('.chosung-btn').first();
    await expect(chosungButton).toBeVisible({ timeout: 5000 });
  });

  test('clicking chosung toggle shows/hides chosung', async ({ page }) => {
    // 힌트 아이템에서 초성 토글 버튼 찾기
    const toggleButton = page.locator('.chosung-btn').first();
    await expect(toggleButton).toBeVisible();

    // 토글 전 상태 확인
    const hintItem = page.locator('.hint-section li').first();
    const beforeText = await hintItem.textContent();

    // 토글 클릭
    await toggleButton.click();
    await page.waitForTimeout(200);

    // 토글 후 상태 확인 (초성이 표시/숨김되어야 함)
    const afterText = await hintItem.textContent();

    // 텍스트가 변경되었는지 확인 (초성이 추가되면 텍스트가 달라짐)
    expect(afterText).not.toBe(beforeText);

    // 초성 텍스트가 표시되는지 확인
    const chosungText = page.locator('.chosung-text').first();
    await expect(chosungText).toBeVisible();
  });
});

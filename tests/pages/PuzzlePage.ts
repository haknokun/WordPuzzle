import { Page, Locator, expect } from '@playwright/test';

export class PuzzlePage {
  readonly page: Page;

  // Locators
  readonly generateButton: Locator;
  readonly puzzleGrid: Locator;
  readonly wordCountInput: Locator;
  readonly levelSelect: Locator;
  readonly welcomeMessage: Locator;
  readonly errorMessage: Locator;
  readonly completeMessage: Locator;
  readonly puzzleInfo: Locator;
  readonly acrossHintSection: Locator;
  readonly downHintSection: Locator;
  readonly controls: Locator;

  constructor(page: Page) {
    this.page = page;

    // Controls
    this.generateButton = page.getByRole('button', { name: '새 퍼즐 생성' });
    this.wordCountInput = page.locator('input[type="number"]');
    this.levelSelect = page.locator('select').first();
    this.controls = page.locator('.controls');

    // Grid
    this.puzzleGrid = page.locator('.puzzle-grid');

    // Hints
    this.acrossHintSection = page.locator('.hint-section').filter({ hasText: '가로 열쇠' });
    this.downHintSection = page.locator('.hint-section').filter({ hasText: '세로 열쇠' });

    // Messages
    this.welcomeMessage = page.locator('.welcome');
    this.errorMessage = page.locator('.error');
    this.completeMessage = page.locator('.complete-message');
    this.puzzleInfo = page.locator('.puzzle-info');
  }

  async goto() {
    await this.page.goto('/');
  }

  async generatePuzzle(options?: { wordCount?: number; level?: string }) {
    if (options?.wordCount) {
      await this.wordCountInput.fill(String(options.wordCount));
    }
    if (options?.level) {
      await this.levelSelect.selectOption(options.level);
    }
    await this.generateButton.click();
    await this.puzzleGrid.waitFor({ state: 'visible', timeout: 30000 });
  }

  async waitForPuzzleLoad() {
    await this.puzzleGrid.waitFor({ state: 'visible', timeout: 30000 });
  }

  // Cell operations
  getCells() {
    return this.page.locator('.puzzle-cell');
  }

  getActiveCells() {
    return this.page.locator('.puzzle-cell.active');
  }

  getSelectedCell() {
    return this.page.locator('.puzzle-cell.selected');
  }

  getHighlightedCells() {
    return this.page.locator('.puzzle-cell.highlighted');
  }

  getCorrectCells() {
    return this.page.locator('.puzzle-cell.correct');
  }

  getIncorrectCells() {
    return this.page.locator('.puzzle-cell.incorrect');
  }

  async clickCell(index: number) {
    await this.getActiveCells().nth(index).click();
  }

  async clickCellAt(row: number, col: number) {
    const cell = this.page.locator(`.puzzle-cell:nth-child(${row * 15 + col + 1})`);
    await cell.click();
  }

  async typeInCell(index: number, text: string) {
    const cell = this.getActiveCells().nth(index);
    const input = cell.locator('input');
    await input.fill(text);
  }

  async typeInSelectedCell(text: string) {
    const selected = this.getSelectedCell();
    const input = selected.locator('input');
    await input.fill(text);
  }

  async getCellValue(index: number) {
    const input = this.getActiveCells().nth(index).locator('input');
    return await input.inputValue();
  }

  // Hint operations
  getAcrossHints() {
    return this.acrossHintSection.locator('li');
  }

  getDownHints() {
    return this.downHintSection.locator('li');
  }

  async clickAcrossHint(index: number) {
    await this.getAcrossHints().nth(index).click();
  }

  async clickDownHint(index: number) {
    await this.getDownHints().nth(index).click();
  }

  getChosungButtons() {
    return this.page.locator('.chosung-btn');
  }

  async toggleChosung(index: number) {
    await this.getChosungButtons().nth(index).click();
  }

  // Keyboard operations
  async pressArrowKey(direction: 'up' | 'down' | 'left' | 'right') {
    const keyMap = {
      up: 'ArrowUp',
      down: 'ArrowDown',
      left: 'ArrowLeft',
      right: 'ArrowRight'
    };
    await this.page.keyboard.press(keyMap[direction]);
  }

  async pressSpace() {
    await this.page.keyboard.press('Space');
  }

  async pressBackspace() {
    await this.page.keyboard.press('Backspace');
  }

  async pressEnter() {
    await this.page.keyboard.press('Enter');
  }

  // Verification methods
  async expectGridVisible() {
    await expect(this.puzzleGrid).toBeVisible();
  }

  async expectWelcomeVisible() {
    await expect(this.welcomeMessage).toBeVisible();
  }

  async expectWelcomeHidden() {
    await expect(this.welcomeMessage).not.toBeVisible();
  }

  async expectErrorVisible() {
    await expect(this.errorMessage).toBeVisible();
  }

  async expectCompleteMessageVisible() {
    await expect(this.completeMessage).toBeVisible();
  }

  async expectHighlightedCellCount(count: number) {
    await expect(this.getHighlightedCells()).toHaveCount(count);
  }

  async expectSelectedCellCount(count: number) {
    await expect(this.getSelectedCell()).toHaveCount(count);
  }

  async getWordCount() {
    const infoText = await this.puzzleInfo.textContent();
    const match = infoText?.match(/총 (\d+)개 단어/);
    return match ? parseInt(match[1]) : 0;
  }

  async getGridSize() {
    const infoText = await this.puzzleInfo.textContent();
    const match = infoText?.match(/그리드 (\d+)x\d+/);
    return match ? parseInt(match[1]) : 0;
  }
}

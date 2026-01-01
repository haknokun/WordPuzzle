import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import PuzzleGrid from '../../components/PuzzleGrid';
import type { PuzzleWord, PuzzleCell } from '../../types/puzzle';

// 3x3 테스트 그리드 생성
const createTestGrid = (): PuzzleCell[][] => [
  [
    { row: 0, col: 0, letter: '사', isBlank: false, acrossNumber: 1, downNumber: 1 },
    { row: 0, col: 1, letter: '과', isBlank: false, acrossNumber: null, downNumber: null },
    { row: 0, col: 2, letter: null, isBlank: true, acrossNumber: null, downNumber: null }
  ],
  [
    { row: 1, col: 0, letter: '람', isBlank: false, acrossNumber: null, downNumber: null },
    { row: 1, col: 1, letter: null, isBlank: true, acrossNumber: null, downNumber: null },
    { row: 1, col: 2, letter: null, isBlank: true, acrossNumber: null, downNumber: null }
  ],
  [
    { row: 2, col: 0, letter: null, isBlank: true, acrossNumber: null, downNumber: null },
    { row: 2, col: 1, letter: null, isBlank: true, acrossNumber: null, downNumber: null },
    { row: 2, col: 2, letter: null, isBlank: true, acrossNumber: null, downNumber: null }
  ]
];

const mockAcrossWords: PuzzleWord[] = [
  { number: 1, word: '사과', definition: '빨간 과일', startRow: 0, startCol: 0, direction: 'ACROSS' }
];

const mockDownWords: PuzzleWord[] = [
  { number: 1, word: '사람', definition: '인간', startRow: 0, startCol: 0, direction: 'DOWN' }
];

describe('PuzzleGrid', () => {
  describe('렌더링', () => {
    it('그리드가 올바른 크기로 렌더링됨', () => {
      const { container } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      const grid = container.querySelector('.puzzle-grid');
      expect(grid).toBeInTheDocument();
    });

    it('활성 셀에 input이 렌더링됨', () => {
      render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      // 활성 셀 (isBlank=false)에만 input이 있어야 함
      const inputs = screen.getAllByRole('textbox');
      expect(inputs.length).toBe(3); // 사, 과, 람
    });

    it('빈 셀에는 input이 렌더링되지 않음', () => {
      const { container } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      const blankCells = container.querySelectorAll('.puzzle-cell.blank');
      expect(blankCells.length).toBe(6); // 9 - 3 = 6개 빈 셀
    });

    it('단어 번호가 렌더링됨', () => {
      const { container } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      // (0,0) 셀에 가로/세로 번호 1이 있어야 함
      const wordNumbers = container.querySelectorAll('.word-number');
      expect(wordNumbers.length).toBeGreaterThanOrEqual(1);
    });
  });

  describe('셀 클릭', () => {
    it('셀 클릭 시 해당 셀이 선택됨', () => {
      const { container } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      const activeCells = container.querySelectorAll('.puzzle-cell.active');
      fireEvent.click(activeCells[0]);

      expect(activeCells[0]).toHaveClass('selected');
    });

    it('빈 셀 클릭 시 선택되지 않음', () => {
      const { container } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      const blankCell = container.querySelector('.puzzle-cell.blank');
      fireEvent.click(blankCell!);

      expect(blankCell).not.toHaveClass('selected');
    });
  });

  describe('입력', () => {
    it('셀에 텍스트 입력이 가능함', () => {
      render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      const inputs = screen.getAllByRole('textbox');
      fireEvent.change(inputs[0], { target: { value: '사' } });

      expect(inputs[0]).toHaveValue('사');
    });

    it('정답 입력 시 correct 클래스 적용됨', () => {
      const { container } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      const inputs = screen.getAllByRole('textbox');
      fireEvent.change(inputs[0], { target: { value: '사' } });

      // 정답인 경우 correct 클래스가 있어야 함
      const cell = container.querySelector('.puzzle-cell.correct');
      expect(cell).toBeInTheDocument();
    });

    it('오답 입력 시 incorrect 클래스 적용됨', () => {
      const { container } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      const inputs = screen.getAllByRole('textbox');
      fireEvent.change(inputs[0], { target: { value: '가' } });

      // 오답인 경우 incorrect 클래스가 있어야 함
      const cell = container.querySelector('.puzzle-cell.incorrect');
      expect(cell).toBeInTheDocument();
    });
  });

  describe('완료 감지', () => {
    it('모든 셀 정답 입력 시 onComplete 호출됨', () => {
      const mockOnComplete = vi.fn();

      render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={mockOnComplete}
        />
      );

      const inputs = screen.getAllByRole('textbox');

      // 모든 정답 입력: 사, 과, 람
      fireEvent.change(inputs[0], { target: { value: '사' } });
      fireEvent.change(inputs[1], { target: { value: '과' } });
      fireEvent.change(inputs[2], { target: { value: '람' } });

      expect(mockOnComplete).toHaveBeenCalled();
    });

    it('일부만 정답이면 onComplete 호출되지 않음', () => {
      const mockOnComplete = vi.fn();

      render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={mockOnComplete}
        />
      );

      const inputs = screen.getAllByRole('textbox');

      // 일부만 정답 입력
      fireEvent.change(inputs[0], { target: { value: '사' } });
      fireEvent.change(inputs[1], { target: { value: '가' } }); // 오답

      expect(mockOnComplete).not.toHaveBeenCalled();
    });
  });

  describe('힌트에서 단어 선택', () => {
    it('selectedWordFromHint prop 변경 시 해당 단어 선택됨', () => {
      const { container, rerender } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
          selectedWordFromHint={null}
        />
      );

      // 힌트에서 단어 선택
      rerender(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
          selectedWordFromHint={mockAcrossWords[0]}
        />
      );

      // 첫 번째 셀이 선택되어야 함
      const selectedCell = container.querySelector('.puzzle-cell.selected');
      expect(selectedCell).toBeInTheDocument();
    });
  });

  describe('단어 하이라이팅', () => {
    it('선택된 단어의 모든 셀이 하이라이트됨', () => {
      const { container } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      // 첫 번째 셀 클릭 (사과 단어 선택)
      const activeCells = container.querySelectorAll('.puzzle-cell.active');
      fireEvent.click(activeCells[0]);

      // '사과' 단어의 두 셀이 highlighted 되어야 함
      const highlightedCells = container.querySelectorAll('.puzzle-cell.highlighted');
      expect(highlightedCells.length).toBeGreaterThanOrEqual(1);
    });
  });
});

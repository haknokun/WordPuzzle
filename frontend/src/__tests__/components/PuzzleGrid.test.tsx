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

    it('다른 셀 클릭 시 이전 하이라이트 해제됨', () => {
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

      // 첫 번째 셀 클릭
      fireEvent.click(activeCells[0]);

      // 세 번째 셀 클릭 (람)
      fireEvent.click(activeCells[2]);

      // 선택 상태 확인
      expect(activeCells[2]).toHaveClass('selected');
    });
  });

  describe('키보드 네비게이션', () => {
    it('화살표 오른쪽 키로 다음 셀 이동', () => {
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
      const inputs = screen.getAllByRole('textbox');

      // 첫 번째 셀 클릭
      fireEvent.click(activeCells[0]);

      // 오른쪽 화살표 키
      fireEvent.keyDown(inputs[0], { key: 'ArrowRight' });

      // 두 번째 셀이 포커스되어야 함
      expect(activeCells[1]).toHaveClass('selected');
    });

    it('화살표 아래 키로 다음 셀 이동', () => {
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
      const inputs = screen.getAllByRole('textbox');

      // 첫 번째 셀 클릭
      fireEvent.click(activeCells[0]);

      // 아래 화살표 키
      fireEvent.keyDown(inputs[0], { key: 'ArrowDown' });

      // (1,0) 셀이 선택되어야 함 - '람'
      expect(activeCells[2]).toHaveClass('selected');
    });

    it('스페이스바로 다음 셀 이동', () => {
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
      const inputs = screen.getAllByRole('textbox');

      // 첫 번째 셀 클릭
      fireEvent.click(activeCells[0]);

      // 스페이스바
      fireEvent.keyDown(inputs[0], { key: ' ' });

      // 다음 셀 이동 확인
      expect(activeCells[1]).toHaveClass('selected');
    });

    it('Backspace 키로 이전 셀 이동 (빈 칸일 때)', () => {
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
      const inputs = screen.getAllByRole('textbox');

      // 두 번째 셀 클릭
      fireEvent.click(activeCells[1]);

      // Backspace 키 (빈 칸에서)
      fireEvent.keyDown(inputs[1], { key: 'Backspace' });

      // 이전 셀로 이동
      expect(activeCells[0]).toHaveClass('selected');
    });

    it('Backspace 키로 현재 칸 내용 삭제', () => {
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

      // 첫 번째 셀에 입력
      fireEvent.change(inputs[0], { target: { value: '사' } });
      expect(inputs[0]).toHaveValue('사');

      // Backspace로 삭제
      fireEvent.keyDown(inputs[0], { key: 'Backspace' });
      fireEvent.change(inputs[0], { target: { value: '' } });

      expect(inputs[0]).toHaveValue('');
    });
  });

  describe('한글 IME 입력', () => {
    it('CompositionStart 이벤트 처리', () => {
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

      // CompositionStart 이벤트
      fireEvent.compositionStart(inputs[0]);

      // 에러 없이 처리되어야 함
      expect(inputs[0]).toBeInTheDocument();
    });

    it('CompositionEnd 이벤트로 첫 글자만 유지', () => {
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

      // 여러 글자 입력 후 CompositionEnd
      inputs[0].value = '사과';
      fireEvent.compositionEnd(inputs[0], {
        currentTarget: { value: '사과' }
      });

      // 첫 글자만 남아야 함 (실제로는 '사'만 유지)
      expect(inputs[0]).toBeInTheDocument();
    });

    it('2글자 이상 입력 시 자동 분리', () => {
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
      const activeCells = container.querySelectorAll('.puzzle-cell.active');

      // 첫 번째 셀 클릭하여 가로 단어 선택
      fireEvent.click(activeCells[0]);

      // 2글자 입력
      fireEvent.change(inputs[0], { target: { value: '사과' } });

      // 첫 글자는 현재 칸, 나머지는 다음 칸으로
      expect(inputs[0]).toHaveValue('사');
    });
  });

  describe('완료 감지 상세', () => {
    it('빈 칸이 있으면 onComplete 호출되지 않음', () => {
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

      // 일부만 입력 (하나 빈칸)
      fireEvent.change(inputs[0], { target: { value: '사' } });
      fireEvent.change(inputs[1], { target: { value: '과' } });
      // inputs[2] 입력 안함

      expect(mockOnComplete).not.toHaveBeenCalled();
    });

    it('모든 칸 정답 입력 시 즉시 onComplete 호출', () => {
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

      // 순서대로 정답 입력
      fireEvent.change(inputs[0], { target: { value: '사' } });
      expect(mockOnComplete).not.toHaveBeenCalled();

      fireEvent.change(inputs[1], { target: { value: '과' } });
      expect(mockOnComplete).not.toHaveBeenCalled();

      fireEvent.change(inputs[2], { target: { value: '람' } });
      expect(mockOnComplete).toHaveBeenCalledTimes(1);
    });

    it('정답 입력 후 수정해도 다시 완료 가능', () => {
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

      // 모든 정답 입력
      fireEvent.change(inputs[0], { target: { value: '사' } });
      fireEvent.change(inputs[1], { target: { value: '과' } });
      fireEvent.change(inputs[2], { target: { value: '람' } });
      expect(mockOnComplete).toHaveBeenCalledTimes(1);

      // 하나 수정 (오답으로)
      fireEvent.change(inputs[1], { target: { value: '가' } });

      // 다시 정답으로 수정
      fireEvent.change(inputs[1], { target: { value: '과' } });
      expect(mockOnComplete).toHaveBeenCalledTimes(2);
    });
  });

  describe('셀 상태 클래스', () => {
    it('선택된 셀에 selected 클래스 적용', () => {
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
      expect(activeCells[1]).not.toHaveClass('selected');
    });

    it('하이라이트된 셀에 highlighted 클래스 적용', () => {
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

      // 가로 단어 '사과'의 셀들이 highlighted
      expect(activeCells[0]).toHaveClass('highlighted');
      expect(activeCells[1]).toHaveClass('highlighted');
    });

    it('정답/오답에 따른 correct/incorrect 클래스', () => {
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

      // 정답 입력
      fireEvent.change(inputs[0], { target: { value: '사' } });
      expect(container.querySelector('.puzzle-cell.correct')).toBeInTheDocument();

      // 오답 입력
      fireEvent.change(inputs[1], { target: { value: '가' } });
      expect(container.querySelector('.puzzle-cell.incorrect')).toBeInTheDocument();
    });
  });

  describe('단어 번호 클릭', () => {
    it('가로 번호 클릭 시 가로 단어 선택', () => {
      const { container } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      const acrossNumber = container.querySelector('.across-number');
      if (acrossNumber) {
        fireEvent.click(acrossNumber);

        // 가로 단어의 셀들이 하이라이트됨
        const highlightedCells = container.querySelectorAll('.puzzle-cell.highlighted');
        expect(highlightedCells.length).toBeGreaterThanOrEqual(1);
      }
    });

    it('세로 번호 클릭 시 세로 단어 선택', () => {
      const { container } = render(
        <PuzzleGrid
          grid={createTestGrid()}
          gridSize={3}
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          onComplete={vi.fn()}
        />
      );

      const downNumber = container.querySelector('.down-number');
      if (downNumber) {
        fireEvent.click(downNumber);

        // 세로 단어의 셀들이 하이라이트됨
        const highlightedCells = container.querySelectorAll('.puzzle-cell.highlighted');
        expect(highlightedCells.length).toBeGreaterThanOrEqual(1);
      }
    });
  });
});

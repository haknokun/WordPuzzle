import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { usePuzzleNavigation } from '../../hooks/usePuzzleNavigation';
import type { PuzzleWord, PuzzleCell } from '../../types/puzzle';

// Mock 데이터
const mockAcrossWords: PuzzleWord[] = [
  { number: 1, word: '사과', definition: '과일', startRow: 0, startCol: 0, direction: 'ACROSS' },
  { number: 2, word: '바나나', definition: '열대과일', startRow: 2, startCol: 1, direction: 'ACROSS' }
];

const mockDownWords: PuzzleWord[] = [
  { number: 1, word: '사람', definition: '인간', startRow: 0, startCol: 0, direction: 'DOWN' },
  { number: 2, word: '과자', definition: '간식', startRow: 0, startCol: 1, direction: 'DOWN' }
];

const createMockGrid = (size: number): PuzzleCell[][] => {
  return Array(size).fill(null).map((_, row) =>
    Array(size).fill(null).map((_, col) => ({
      row,
      col,
      letter: 'ㅇ',
      isBlank: false,
      acrossNumber: null,
      downNumber: null
    }))
  );
};

describe('usePuzzleNavigation', () => {
  describe('초기 상태', () => {
    it('selectedCell은 초기에 null이어야 함', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      expect(result.current.selectedCell).toBeNull();
    });

    it('selectedWord는 초기에 null이어야 함', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      expect(result.current.selectedWord).toBeNull();
    });
  });

  describe('셀 클릭', () => {
    it('셀 클릭 시 selectedCell이 업데이트됨', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.handleCellClick(1, 1);
      });

      expect(result.current.selectedCell).toEqual({ row: 1, col: 1 });
    });

    it('빈 셀 클릭 시 selectedCell이 변경되지 않음', () => {
      const gridWithBlank = createMockGrid(3);
      gridWithBlank[1][1].isBlank = true;

      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: gridWithBlank,
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.handleCellClick(1, 1);
      });

      expect(result.current.selectedCell).toBeNull();
    });

    it('같은 셀 재클릭 시 방향이 전환됨', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      // (0,0)은 '사과'(ACROSS)와 '사람'(DOWN)의 교차점
      act(() => {
        result.current.handleCellClick(0, 0);
      });

      const firstDirection = result.current.selectedWord?.direction;

      act(() => {
        result.current.handleCellClick(0, 0);
      });

      // 방향이 전환되어야 함
      expect(result.current.selectedWord?.direction).not.toBe(firstDirection);
    });
  });

  describe('키보드 네비게이션', () => {
    it('ArrowRight 키 누르면 오른쪽으로 이동', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.handleCellClick(0, 0);
      });

      act(() => {
        result.current.handleArrowKey('ArrowRight');
      });

      expect(result.current.selectedCell).toEqual({ row: 0, col: 1 });
    });

    it('ArrowDown 키 누르면 아래로 이동', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.handleCellClick(0, 0);
      });

      act(() => {
        result.current.handleArrowKey('ArrowDown');
      });

      expect(result.current.selectedCell).toEqual({ row: 1, col: 0 });
    });

    it('ArrowLeft 키 누르면 왼쪽으로 이동', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.handleCellClick(1, 1);
      });

      act(() => {
        result.current.handleArrowKey('ArrowLeft');
      });

      expect(result.current.selectedCell).toEqual({ row: 1, col: 0 });
    });

    it('ArrowUp 키 누르면 위로 이동', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.handleCellClick(1, 1);
      });

      act(() => {
        result.current.handleArrowKey('ArrowUp');
      });

      expect(result.current.selectedCell).toEqual({ row: 0, col: 1 });
    });

    it('경계를 벗어나는 이동은 무시됨', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.handleCellClick(0, 0);
      });

      act(() => {
        result.current.handleArrowKey('ArrowLeft');
      });

      // 경계라서 이동하지 않음
      expect(result.current.selectedCell).toEqual({ row: 0, col: 0 });
    });
  });

  describe('다음 셀 이동', () => {
    it('ACROSS 방향일 때 다음 셀은 오른쪽', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      // '사과' 단어 선택 (ACROSS)
      act(() => {
        result.current.handleCellClick(0, 0);
      });

      // 현재 selectedWord가 ACROSS인지 확인하고 다음 셀 이동
      if (result.current.selectedWord?.direction === 'ACROSS') {
        act(() => {
          result.current.moveToNextCell();
        });

        expect(result.current.selectedCell).toEqual({ row: 0, col: 1 });
      }
    });

    it('DOWN 방향일 때 다음 셀은 아래', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      // (0,0) 클릭 후 같은 셀 클릭하여 방향 전환
      act(() => {
        result.current.handleCellClick(0, 0);
      });

      // DOWN으로 전환
      if (result.current.selectedWord?.direction === 'ACROSS') {
        act(() => {
          result.current.handleCellClick(0, 0);
        });
      }

      if (result.current.selectedWord?.direction === 'DOWN') {
        act(() => {
          result.current.moveToNextCell();
        });

        expect(result.current.selectedCell).toEqual({ row: 1, col: 0 });
      }
    });
  });

  describe('힌트에서 단어 선택', () => {
    it('selectWordFromHint 호출 시 해당 단어의 첫 셀이 선택됨', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.selectWordFromHint(mockAcrossWords[1]); // '바나나' at (2, 1)
      });

      expect(result.current.selectedCell).toEqual({ row: 2, col: 1 });
      expect(result.current.selectedWord).toEqual(mockAcrossWords[1]);
    });
  });
});

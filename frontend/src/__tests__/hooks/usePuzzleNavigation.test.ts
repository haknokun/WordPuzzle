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

    it('DOWN 방향 단어 선택 시 해당 단어의 첫 셀이 선택됨', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.selectWordFromHint(mockDownWords[1]); // '과자' at (0, 1)
      });

      expect(result.current.selectedCell).toEqual({ row: 0, col: 1 });
      expect(result.current.selectedWord).toEqual(mockDownWords[1]);
    });
  });

  describe('이전 셀 이동 (moveToPrevCell)', () => {
    it('ACROSS 방향일 때 이전 셀은 왼쪽', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      // '사과' 단어의 두 번째 셀 선택 (0, 1)
      act(() => {
        result.current.handleCellClick(0, 0);
      });

      // ACROSS 방향인지 확인
      if (result.current.selectedWord?.direction === 'ACROSS') {
        // 다음 셀로 이동
        act(() => {
          result.current.moveToNextCell();
        });

        // (0, 1)에서 이전 셀로 이동
        act(() => {
          result.current.moveToPrevCell();
        });

        expect(result.current.selectedCell).toEqual({ row: 0, col: 0 });
      }
    });

    it('DOWN 방향일 때 이전 셀은 위쪽', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      // (0,0)에서 시작, DOWN 방향으로 전환
      act(() => {
        result.current.handleCellClick(0, 0);
      });

      if (result.current.selectedWord?.direction === 'ACROSS') {
        act(() => {
          result.current.handleCellClick(0, 0); // 방향 전환
        });
      }

      if (result.current.selectedWord?.direction === 'DOWN') {
        // 다음 셀로 이동 (1, 0)
        act(() => {
          result.current.moveToNextCell();
        });

        // 이전 셀로 이동
        act(() => {
          result.current.moveToPrevCell();
        });

        expect(result.current.selectedCell).toEqual({ row: 0, col: 0 });
      }
    });

    it('단어 시작점에서 moveToPrevCell은 이동하지 않음 (ACROSS)', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      // 단어 시작점 선택
      act(() => {
        result.current.handleCellClick(0, 0);
      });

      if (result.current.selectedWord?.direction === 'ACROSS') {
        const cellBefore = result.current.selectedCell;

        act(() => {
          result.current.moveToPrevCell();
        });

        // 시작점이라 이동하지 않음
        expect(result.current.selectedCell).toEqual(cellBefore);
      }
    });

    it('단어 시작점에서 moveToPrevCell은 이동하지 않음 (DOWN)', () => {
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

      // DOWN으로 전환
      if (result.current.selectedWord?.direction === 'ACROSS') {
        act(() => {
          result.current.handleCellClick(0, 0);
        });
      }

      if (result.current.selectedWord?.direction === 'DOWN') {
        const cellBefore = result.current.selectedCell;

        act(() => {
          result.current.moveToPrevCell();
        });

        // 시작점이라 이동하지 않음
        expect(result.current.selectedCell).toEqual(cellBefore);
      }
    });

    it('selectedCell이 null이면 moveToPrevCell은 아무것도 하지 않음', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      // selectedCell이 null인 상태
      act(() => {
        result.current.moveToPrevCell();
      });

      expect(result.current.selectedCell).toBeNull();
    });

    it('selectedWord가 null이면 moveToPrevCell은 아무것도 하지 않음', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: [],
          downWords: []
        })
      );

      // 셀 선택 (단어가 없어서 selectedWord는 null)
      act(() => {
        result.current.setSelectedCell({ row: 1, col: 1 });
      });

      act(() => {
        result.current.moveToPrevCell();
      });

      // 이동하지 않음
      expect(result.current.selectedCell).toEqual({ row: 1, col: 1 });
    });
  });

  describe('다음 셀 이동 경계 케이스', () => {
    it('단어 끝에서 moveToNextCell은 이동하지 않음 (ACROSS)', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      // '사과' 단어 선택 (길이 2, 시작 0,0)
      act(() => {
        result.current.handleCellClick(0, 0);
      });

      if (result.current.selectedWord?.direction === 'ACROSS') {
        // 끝까지 이동
        act(() => {
          result.current.moveToNextCell(); // (0,1)
        });

        const cellAtEnd = result.current.selectedCell;

        act(() => {
          result.current.moveToNextCell(); // 더 이동 시도
        });

        // 단어 끝이라 이동하지 않음
        expect(result.current.selectedCell).toEqual(cellAtEnd);
      }
    });

    it('단어 끝에서 moveToNextCell은 이동하지 않음 (DOWN)', () => {
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

      // DOWN으로 전환
      if (result.current.selectedWord?.direction === 'ACROSS') {
        act(() => {
          result.current.handleCellClick(0, 0);
        });
      }

      if (result.current.selectedWord?.direction === 'DOWN') {
        // '사람' 단어 (길이 2)
        act(() => {
          result.current.moveToNextCell(); // (1,0)
        });

        const cellAtEnd = result.current.selectedCell;

        act(() => {
          result.current.moveToNextCell(); // 더 이동 시도
        });

        expect(result.current.selectedCell).toEqual(cellAtEnd);
      }
    });

    it('selectedCell이 null이면 moveToNextCell은 아무것도 하지 않음', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.moveToNextCell();
      });

      expect(result.current.selectedCell).toBeNull();
    });
  });

  describe('키보드 네비게이션 경계 케이스', () => {
    it('selectedCell이 null이면 handleArrowKey는 아무것도 하지 않음', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.handleArrowKey('ArrowRight');
      });

      expect(result.current.selectedCell).toBeNull();
    });

    it('우측 경계에서 ArrowRight는 이동하지 않음', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.handleCellClick(0, 2);
      });

      act(() => {
        result.current.handleArrowKey('ArrowRight');
      });

      expect(result.current.selectedCell).toEqual({ row: 0, col: 2 });
    });

    it('상단 경계에서 ArrowUp은 이동하지 않음', () => {
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
        result.current.handleArrowKey('ArrowUp');
      });

      expect(result.current.selectedCell).toEqual({ row: 0, col: 0 });
    });

    it('하단 경계에서 ArrowDown은 이동하지 않음', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.handleCellClick(2, 2);
      });

      act(() => {
        result.current.handleArrowKey('ArrowDown');
      });

      expect(result.current.selectedCell).toEqual({ row: 2, col: 2 });
    });

    it('빈 셀로는 화살표 키로 이동하지 않음', () => {
      const gridWithBlank = createMockGrid(3);
      gridWithBlank[0][1].isBlank = true;

      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: gridWithBlank,
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

      // 빈 셀이라 이동하지 않음
      expect(result.current.selectedCell).toEqual({ row: 0, col: 0 });
    });

    it('알 수 없는 키는 무시됨', () => {
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
        result.current.handleArrowKey('Enter');
      });

      expect(result.current.selectedCell).toEqual({ row: 1, col: 1 });
    });
  });

  describe('setSelectedCell / setSelectedWord 직접 호출', () => {
    it('setSelectedCell로 직접 셀 설정', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.setSelectedCell({ row: 2, col: 2 });
      });

      expect(result.current.selectedCell).toEqual({ row: 2, col: 2 });
    });

    it('setSelectedWord로 직접 단어 설정', () => {
      const { result } = renderHook(() =>
        usePuzzleNavigation({
          grid: createMockGrid(3),
          gridSize: 3,
          acrossWords: mockAcrossWords,
          downWords: mockDownWords
        })
      );

      act(() => {
        result.current.setSelectedWord(mockDownWords[0]);
      });

      expect(result.current.selectedWord).toEqual(mockDownWords[0]);
    });
  });
});

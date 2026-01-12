import { renderHook, act } from '@testing-library/react';
import { usePuzzleInput } from '../../hooks/usePuzzleInput';
import type { PuzzleCell, PuzzleWord } from '../../types/puzzle';

describe('usePuzzleInput', () => {
  // 테스트용 그리드 생성
  const createMockGrid = (size: number): PuzzleCell[][] => {
    return Array(size).fill(null).map((_, row) =>
      Array(size).fill(null).map((_, col) => ({
        row,
        col,
        letter: '',
        isBlank: false,
        acrossNumber: null,
        downNumber: null
      }))
    );
  };

  // 기본 props 생성
  const createDefaultProps = (overrides: Partial<Parameters<typeof usePuzzleInput>[0]> = {}) => {
    const grid = createMockGrid(5);
    // 일부 셀을 blank로 설정
    grid[0][0].isBlank = true;
    grid[0][4].isBlank = true;

    const inputRefs = { current: Array(5).fill(null).map(() => Array(5).fill(null)) };

    return {
      grid,
      gridSize: 5,
      selectedWord: null,
      selectedCell: null,
      setSelectedCell: vi.fn(),
      inputRefs,
      moveToNextCell: vi.fn(),
      moveToPrevCell: vi.fn(),
      handleArrowKey: vi.fn(),
      ...overrides
    };
  };

  describe('초기화', () => {
    it('userInputs가 gridSize에 맞게 초기화된다', () => {
      const props = createDefaultProps();
      const { result } = renderHook(() => usePuzzleInput(props));

      expect(result.current.userInputs).toHaveLength(5);
      expect(result.current.userInputs[0]).toHaveLength(5);
      expect(result.current.userInputs[0][0]).toBe('');
    });

    it('isComposing이 false로 초기화된다', () => {
      const props = createDefaultProps();
      const { result } = renderHook(() => usePuzzleInput(props));

      expect(result.current.isComposing.current).toBe(false);
    });
  });

  describe('updateUserInput', () => {
    it('특정 셀의 값을 업데이트한다', () => {
      const props = createDefaultProps();
      const { result } = renderHook(() => usePuzzleInput(props));

      act(() => {
        result.current.updateUserInput(1, 1, '가');
      });

      expect(result.current.userInputs[1][1]).toBe('가');
    });

    it('다른 셀의 값은 변경하지 않는다', () => {
      const props = createDefaultProps();
      const { result } = renderHook(() => usePuzzleInput(props));

      act(() => {
        result.current.updateUserInput(1, 1, '가');
        result.current.updateUserInput(2, 2, '나');
      });

      expect(result.current.userInputs[1][1]).toBe('가');
      expect(result.current.userInputs[2][2]).toBe('나');
      expect(result.current.userInputs[0][1]).toBe('');
    });
  });

  describe('getNextCell', () => {
    it('selectedWord가 없으면 null을 반환한다', () => {
      const props = createDefaultProps();
      const { result } = renderHook(() => usePuzzleInput(props));

      const nextCell = result.current.getNextCell(1, 1);
      expect(nextCell).toBeNull();
    });

    it('ACROSS 방향에서 다음 셀을 반환한다', () => {
      const selectedWord: PuzzleWord = {
        number: 1,
        word: '가나다',
        definition: '테스트',
        startRow: 1,
        startCol: 1,
        direction: 'ACROSS'
      };
      const props = createDefaultProps({ selectedWord });
      const { result } = renderHook(() => usePuzzleInput(props));

      const nextCell = result.current.getNextCell(1, 1);
      expect(nextCell).toEqual({ row: 1, col: 2 });
    });

    it('DOWN 방향에서 다음 셀을 반환한다', () => {
      const selectedWord: PuzzleWord = {
        number: 1,
        word: '가나다',
        definition: '테스트',
        startRow: 1,
        startCol: 1,
        direction: 'DOWN'
      };
      const props = createDefaultProps({ selectedWord });
      const { result } = renderHook(() => usePuzzleInput(props));

      const nextCell = result.current.getNextCell(1, 1);
      expect(nextCell).toEqual({ row: 2, col: 1 });
    });

    it('단어 끝에서는 null을 반환한다 (ACROSS)', () => {
      const selectedWord: PuzzleWord = {
        number: 1,
        word: '가나',
        definition: '테스트',
        startRow: 1,
        startCol: 1,
        direction: 'ACROSS'
      };
      const props = createDefaultProps({ selectedWord });
      const { result } = renderHook(() => usePuzzleInput(props));

      const nextCell = result.current.getNextCell(1, 2); // 단어 마지막 셀
      expect(nextCell).toBeNull();
    });

    it('단어 끝에서는 null을 반환한다 (DOWN)', () => {
      const selectedWord: PuzzleWord = {
        number: 1,
        word: '가나',
        definition: '테스트',
        startRow: 1,
        startCol: 1,
        direction: 'DOWN'
      };
      const props = createDefaultProps({ selectedWord });
      const { result } = renderHook(() => usePuzzleInput(props));

      const nextCell = result.current.getNextCell(2, 1); // 단어 마지막 셀
      expect(nextCell).toBeNull();
    });

    it('다음 셀이 blank이면 null을 반환한다', () => {
      const grid = createMockGrid(5);
      grid[1][2].isBlank = true; // 다음 셀을 blank로

      const selectedWord: PuzzleWord = {
        number: 1,
        word: '가나다',
        definition: '테스트',
        startRow: 1,
        startCol: 1,
        direction: 'ACROSS'
      };
      const props = createDefaultProps({ grid, selectedWord });
      const { result } = renderHook(() => usePuzzleInput(props));

      const nextCell = result.current.getNextCell(1, 1);
      expect(nextCell).toBeNull();
    });
  });

  describe('handleCompositionStart/End', () => {
    it('handleCompositionStart에서 isComposing이 true가 된다', () => {
      const props = createDefaultProps();
      const { result } = renderHook(() => usePuzzleInput(props));

      act(() => {
        result.current.handleCompositionStart();
      });

      expect(result.current.isComposing.current).toBe(true);
    });

    it('handleCompositionEnd에서 isComposing이 false가 된다', () => {
      const props = createDefaultProps();
      const { result } = renderHook(() => usePuzzleInput(props));

      const mockEvent = {
        currentTarget: { value: '가' }
      } as React.CompositionEvent<HTMLInputElement>;

      act(() => {
        result.current.handleCompositionStart();
        result.current.handleCompositionEnd(1, 1, mockEvent);
      });

      expect(result.current.isComposing.current).toBe(false);
    });

    it('handleCompositionEnd에서 값이 업데이트된다', () => {
      const props = createDefaultProps();
      const { result } = renderHook(() => usePuzzleInput(props));

      const mockEvent = {
        currentTarget: { value: '가' }
      } as React.CompositionEvent<HTMLInputElement>;

      act(() => {
        result.current.handleCompositionEnd(1, 1, mockEvent);
      });

      expect(result.current.userInputs[1][1]).toBe('가');
    });

    it('2글자 이상 입력 시 다음 셀로 이동한다', () => {
      const setSelectedCell = vi.fn();
      const inputRefs = { current: Array(5).fill(null).map(() => Array(5).fill(null)) };
      const mockNextInput = { value: '', focus: vi.fn() };
      inputRefs.current[1][2] = mockNextInput as unknown as HTMLInputElement;

      const selectedWord: PuzzleWord = {
        number: 1,
        word: '가나다',
        definition: '테스트',
        startRow: 1,
        startCol: 1,
        direction: 'ACROSS'
      };
      const props = createDefaultProps({ selectedWord, setSelectedCell, inputRefs });
      const { result } = renderHook(() => usePuzzleInput(props));

      const mockEvent = {
        currentTarget: { value: '가나' }
      } as React.CompositionEvent<HTMLInputElement>;

      act(() => {
        result.current.handleCompositionEnd(1, 1, mockEvent);
      });

      expect(setSelectedCell).toHaveBeenCalledWith({ row: 1, col: 2 });
      expect(mockNextInput.focus).toHaveBeenCalled();
      expect(mockNextInput.value).toBe('나');
    });
  });

  describe('handleInput', () => {
    it('blank 셀에서는 처리하지 않는다', () => {
      const grid = createMockGrid(5);
      grid[1][1].isBlank = true;
      const props = createDefaultProps({ grid });
      const { result } = renderHook(() => usePuzzleInput(props));

      const mockEvent = {
        target: { value: '가' }
      } as React.ChangeEvent<HTMLInputElement>;

      act(() => {
        result.current.handleInput(1, 1, mockEvent);
      });

      expect(result.current.userInputs[1][1]).toBe('');
    });

    it('한글 조합 중에는 처리하지 않는다', () => {
      const props = createDefaultProps();
      const { result } = renderHook(() => usePuzzleInput(props));

      const mockEvent = {
        target: { value: '가' }
      } as React.ChangeEvent<HTMLInputElement>;

      act(() => {
        result.current.handleCompositionStart();
        result.current.handleInput(1, 1, mockEvent);
      });

      expect(result.current.userInputs[1][1]).toBe('');
    });

    it('단일 글자 입력 시 값을 업데이트한다', () => {
      const props = createDefaultProps();
      const { result } = renderHook(() => usePuzzleInput(props));

      const mockEvent = {
        target: { value: '가' }
      } as React.ChangeEvent<HTMLInputElement>;

      act(() => {
        result.current.handleInput(1, 1, mockEvent);
      });

      expect(result.current.userInputs[1][1]).toBe('가');
    });
  });

  describe('handleKeyDown', () => {
    it('스페이스바 누르면 moveToNextCell 호출', () => {
      const moveToNextCell = vi.fn();
      const props = createDefaultProps({ moveToNextCell });
      const { result } = renderHook(() => usePuzzleInput(props));

      const mockEvent = {
        key: ' ',
        preventDefault: vi.fn()
      } as unknown as React.KeyboardEvent;

      act(() => {
        result.current.handleKeyDown(mockEvent, 1, 1);
      });

      expect(mockEvent.preventDefault).toHaveBeenCalled();
      expect(moveToNextCell).toHaveBeenCalled();
    });

    it('빈 셀에서 Backspace 누르면 moveToPrevCell 호출', () => {
      const moveToPrevCell = vi.fn();
      const inputRefs = { current: Array(5).fill(null).map(() => Array(5).fill(null)) };
      inputRefs.current[1][1] = { value: '' } as HTMLInputElement;

      const props = createDefaultProps({ moveToPrevCell, inputRefs });
      const { result } = renderHook(() => usePuzzleInput(props));

      const mockEvent = {
        key: 'Backspace'
      } as React.KeyboardEvent;

      act(() => {
        result.current.handleKeyDown(mockEvent, 1, 1);
      });

      expect(moveToPrevCell).toHaveBeenCalled();
    });

    it('화살표 키 누르면 handleArrowKey 호출', () => {
      const handleArrowKey = vi.fn();
      const props = createDefaultProps({ handleArrowKey });
      const { result } = renderHook(() => usePuzzleInput(props));

      ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].forEach(key => {
        const mockEvent = { key } as React.KeyboardEvent;

        act(() => {
          result.current.handleKeyDown(mockEvent, 1, 1);
        });

        expect(handleArrowKey).toHaveBeenCalledWith(key);
      });
    });
  });
});

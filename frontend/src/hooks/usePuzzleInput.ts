import { useState, useRef, useCallback } from 'react';
import type { PuzzleCell, PuzzleWord } from '../types/puzzle';

interface UsePuzzleInputProps {
  grid: PuzzleCell[][];
  gridSize: number;
  selectedWord: PuzzleWord | null;
  selectedCell: { row: number; col: number } | null;
  setSelectedCell: React.Dispatch<React.SetStateAction<{ row: number; col: number } | null>>;
  inputRefs: React.MutableRefObject<(HTMLInputElement | null)[][]>;
  moveToNextCell: () => void;
  moveToPrevCell: () => void;
  handleArrowKey: (key: string) => void;
}

interface UsePuzzleInputReturn {
  userInputs: string[][];
  isComposing: React.MutableRefObject<boolean>;
  updateUserInput: (row: number, col: number, value: string) => void;
  handleInput: (row: number, col: number, e: React.ChangeEvent<HTMLInputElement>) => void;
  handleCompositionStart: () => void;
  handleCompositionEnd: (row: number, col: number, e: React.CompositionEvent<HTMLInputElement>) => void;
  handleKeyDown: (e: React.KeyboardEvent, row: number, col: number) => void;
  getNextCell: (row: number, col: number) => { row: number; col: number } | null;
}

export function usePuzzleInput({
  grid,
  gridSize,
  selectedWord,
  selectedCell,
  setSelectedCell,
  inputRefs,
  moveToNextCell,
  moveToPrevCell,
  handleArrowKey
}: UsePuzzleInputProps): UsePuzzleInputReturn {
  const [userInputs, setUserInputs] = useState<string[][]>(() =>
    Array(gridSize).fill(null).map(() => Array(gridSize).fill(''))
  );
  const isComposing = useRef(false);

  const updateUserInput = useCallback((row: number, col: number, value: string) => {
    setUserInputs(prev => {
      const newInputs = prev.map(r => [...r]); // 깊은 복사
      newInputs[row][col] = value;
      return newInputs;
    });
  }, []);

  const getNextCell = useCallback((row: number, col: number): { row: number; col: number } | null => {
    if (!selectedWord) return null;

    let nextRow = row;
    let nextCol = col;

    if (selectedWord.direction === 'ACROSS') {
      nextCol = col + 1;
      if (nextCol >= selectedWord.startCol + selectedWord.word.length) {
        return null;
      }
    } else {
      nextRow = row + 1;
      if (nextRow >= selectedWord.startRow + selectedWord.word.length) {
        return null;
      }
    }

    if (nextRow < gridSize && nextCol < gridSize && !grid[nextRow][nextCol].isBlank) {
      return { row: nextRow, col: nextCol };
    }
    return null;
  }, [selectedWord, gridSize, grid]);

  const handleInput = useCallback((row: number, col: number, e: React.ChangeEvent<HTMLInputElement>) => {
    if (grid[row][col].isBlank) return;

    // 한글 조합 중에는 처리하지 않음 (CompositionEnd에서 처리)
    if (isComposing.current) return;

    const value = e.target.value;

    // 2글자 이상이면 첫 글자는 현재 칸에, 나머지는 다음 칸으로
    if (value.length >= 2) {
      const firstChar = value.charAt(0);
      const restChars = value.slice(1);

      // 현재 칸은 첫 글자만
      e.target.value = firstChar;
      updateUserInput(row, col, firstChar);

      // 다음 셀로 이동하고 나머지 글자 입력
      const nextCell = getNextCell(row, col);
      if (nextCell) {
        setSelectedCell(nextCell);
        const nextInput = inputRefs.current[nextCell.row][nextCell.col];
        if (nextInput) {
          nextInput.value = restChars;
          nextInput.focus();
          updateUserInput(nextCell.row, nextCell.col, restChars);
        }
      }
    } else {
      updateUserInput(row, col, value);
    }
  }, [grid, updateUserInput, getNextCell, setSelectedCell, inputRefs]);

  const handleCompositionStart = useCallback(() => {
    isComposing.current = true;
  }, []);

  const handleCompositionEnd = useCallback((row: number, col: number, e: React.CompositionEvent<HTMLInputElement>) => {
    isComposing.current = false;
    const value = e.currentTarget.value;

    if (value.length >= 1) {
      const firstChar = value.charAt(0);
      updateUserInput(row, col, firstChar);
      e.currentTarget.value = firstChar;

      // 2글자 이상이면 다음 셀로 이동
      if (value.length >= 2) {
        const restChars = value.slice(1);
        const nextCell = getNextCell(row, col);
        if (nextCell) {
          setSelectedCell(nextCell);
          const nextInput = inputRefs.current[nextCell.row][nextCell.col];
          if (nextInput) {
            nextInput.value = restChars;
            nextInput.focus();
            updateUserInput(nextCell.row, nextCell.col, restChars);
          }
        }
      }
    }
  }, [updateUserInput, getNextCell, setSelectedCell, inputRefs]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent, row: number, col: number) => {
    if (e.key === ' ') {
      // 스페이스바: 다음 칸으로 이동
      e.preventDefault();
      moveToNextCell();
    } else if (e.key === 'Backspace') {
      const currentInput = inputRefs.current[row][col];
      const currentValue = currentInput?.value || '';

      if (!currentValue) {
        // 현재 칸이 비어있으면 이전 칸으로 이동
        moveToPrevCell();
      } else {
        // 현재 칸 내용 삭제 후 상태 업데이트
        setTimeout(() => {
          updateUserInput(row, col, currentInput?.value || '');
        }, 0);
      }
    } else if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
      handleArrowKey(e.key);
    }
  }, [moveToNextCell, moveToPrevCell, handleArrowKey, updateUserInput, inputRefs]);

  return {
    userInputs,
    isComposing,
    updateUserInput,
    handleInput,
    handleCompositionStart,
    handleCompositionEnd,
    handleKeyDown,
    getNextCell
  };
}

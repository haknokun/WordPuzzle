import { useState, useCallback } from 'react';
import type { PuzzleCell, PuzzleWord } from '../types/puzzle';
import { findWordAtCell } from '../utils/puzzleUtils';

interface UsePuzzleNavigationProps {
  grid: PuzzleCell[][];
  gridSize: number;
  acrossWords: PuzzleWord[];
  downWords: PuzzleWord[];
}

interface UsePuzzleNavigationReturn {
  selectedCell: { row: number; col: number } | null;
  selectedWord: PuzzleWord | null;
  handleCellClick: (row: number, col: number) => void;
  handleArrowKey: (key: string) => void;
  moveToNextCell: () => void;
  moveToPrevCell: () => void;
  selectWordFromHint: (word: PuzzleWord) => void;
  setSelectedCell: React.Dispatch<React.SetStateAction<{ row: number; col: number } | null>>;
  setSelectedWord: React.Dispatch<React.SetStateAction<PuzzleWord | null>>;
}

export function usePuzzleNavigation({
  grid,
  gridSize,
  acrossWords,
  downWords
}: UsePuzzleNavigationProps): UsePuzzleNavigationReturn {
  const [selectedCell, setSelectedCell] = useState<{ row: number; col: number } | null>(null);
  const [selectedWord, setSelectedWord] = useState<PuzzleWord | null>(null);

  const handleCellClick = useCallback((row: number, col: number) => {
    if (grid[row][col].isBlank) return;

    // 같은 셀을 다시 클릭했는지 확인
    const isSameCell = selectedCell?.row === row && selectedCell?.col === col;

    setSelectedCell({ row, col });

    // 해당 셀이 속한 단어 찾기 (같은 셀이면 방향 전환)
    const word = findWordAtCell(
      row,
      col,
      acrossWords,
      downWords,
      isSameCell ? selectedWord?.direction : undefined
    );
    setSelectedWord(word);
  }, [grid, selectedCell, selectedWord, acrossWords, downWords]);

  const handleArrowKey = useCallback((key: string) => {
    if (!selectedCell) return;

    const { row, col } = selectedCell;
    let newRow = row;
    let newCol = col;

    switch (key) {
      case 'ArrowUp':
        newRow = row - 1;
        break;
      case 'ArrowDown':
        newRow = row + 1;
        break;
      case 'ArrowLeft':
        newCol = col - 1;
        break;
      case 'ArrowRight':
        newCol = col + 1;
        break;
      default:
        return;
    }

    // 경계 체크 및 빈 셀 체크
    if (
      newRow >= 0 &&
      newRow < gridSize &&
      newCol >= 0 &&
      newCol < gridSize &&
      !grid[newRow][newCol].isBlank
    ) {
      setSelectedCell({ row: newRow, col: newCol });
    }
  }, [selectedCell, grid, gridSize]);

  const moveToNextCell = useCallback(() => {
    if (!selectedCell || !selectedWord) return;

    const { row, col } = selectedCell;
    let nextRow = row;
    let nextCol = col;

    if (selectedWord.direction === 'ACROSS') {
      nextCol = col + 1;
      // 단어 범위 체크
      if (nextCol >= selectedWord.startCol + selectedWord.word.length) {
        return;
      }
    } else {
      nextRow = row + 1;
      // 단어 범위 체크
      if (nextRow >= selectedWord.startRow + selectedWord.word.length) {
        return;
      }
    }

    if (
      nextRow >= 0 &&
      nextRow < gridSize &&
      nextCol >= 0 &&
      nextCol < gridSize &&
      !grid[nextRow][nextCol].isBlank
    ) {
      setSelectedCell({ row: nextRow, col: nextCol });
    }
  }, [selectedCell, selectedWord, grid, gridSize]);

  const moveToPrevCell = useCallback(() => {
    if (!selectedCell || !selectedWord) return;

    const { row, col } = selectedCell;
    let prevRow = row;
    let prevCol = col;

    if (selectedWord.direction === 'ACROSS') {
      prevCol = col - 1;
      // 단어 시작점 체크
      if (prevCol < selectedWord.startCol) {
        return;
      }
    } else {
      prevRow = row - 1;
      // 단어 시작점 체크
      if (prevRow < selectedWord.startRow) {
        return;
      }
    }

    if (
      prevRow >= 0 &&
      prevCol >= 0 &&
      !grid[prevRow][prevCol].isBlank
    ) {
      setSelectedCell({ row: prevRow, col: prevCol });
    }
  }, [selectedCell, selectedWord, grid]);

  const selectWordFromHint = useCallback((word: PuzzleWord) => {
    setSelectedCell({ row: word.startRow, col: word.startCol });
    setSelectedWord(word);
  }, []);

  return {
    selectedCell,
    selectedWord,
    handleCellClick,
    handleArrowKey,
    moveToNextCell,
    moveToPrevCell,
    selectWordFromHint,
    setSelectedCell,
    setSelectedWord
  };
}

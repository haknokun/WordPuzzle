import { useState, useRef, useEffect, useCallback } from 'react';
import type { PuzzleCell, PuzzleWord } from '../types/puzzle';
import {
  isCellInSelectedWord,
  checkCellCorrect,
  checkPuzzleCompletion
} from '../utils/puzzleUtils';
import { usePuzzleNavigation } from '../hooks/usePuzzleNavigation';
import './PuzzleGrid.css';

interface Props {
  grid: PuzzleCell[][];
  gridSize: number;
  acrossWords: PuzzleWord[];
  downWords: PuzzleWord[];
  onComplete: () => void;
  selectedWordFromHint?: PuzzleWord | null;
  onWordSelect?: (word: PuzzleWord | null) => void;
}

export default function PuzzleGrid({ grid, gridSize, acrossWords, downWords, onComplete, selectedWordFromHint, onWordSelect }: Props) {
  const [userInputs, setUserInputs] = useState<string[][]>(() =>
    Array(gridSize).fill(null).map(() => Array(gridSize).fill(''))
  );
  const inputRefs = useRef<(HTMLInputElement | null)[][]>(
    Array(gridSize).fill(null).map(() => Array(gridSize).fill(null))
  );
  const isComposing = useRef(false);

  // 네비게이션 훅 사용
  const {
    selectedCell,
    selectedWord,
    handleCellClick: navHandleCellClick,
    handleArrowKey,
    moveToNextCell,
    moveToPrevCell,
    selectWordFromHint,
    setSelectedCell,
    setSelectedWord
  } = usePuzzleNavigation({ grid, gridSize, acrossWords, downWords });

  // 셀 클릭 시 포커스 처리 추가
  const handleCellClick = useCallback((row: number, col: number) => {
    navHandleCellClick(row, col);
    inputRefs.current[row][col]?.focus();
  }, [navHandleCellClick]);

  const handleNumberClick = (e: React.MouseEvent, row: number, col: number, direction: 'ACROSS' | 'DOWN') => {
    e.stopPropagation();

    const words = direction === 'ACROSS' ? acrossWords : downWords;
    const word = words.find(w => w.startRow === row && w.startCol === col);

    if (word) {
      setSelectedCell({ row, col });
      setSelectedWord(word);
      inputRefs.current[row][col]?.focus();
    }
  };

  const updateUserInput = useCallback((row: number, col: number, value: string) => {
    setUserInputs(prev => {
      const newInputs = prev.map(r => [...r]); // 깊은 복사
      newInputs[row][col] = value;
      return newInputs;
    });
  }, []);

  const handleInput = (row: number, col: number, e: React.ChangeEvent<HTMLInputElement>) => {
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
  };

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

  const handleCompositionStart = () => {
    isComposing.current = true;
  };

  const handleCompositionEnd = (row: number, col: number, e: React.CompositionEvent<HTMLInputElement>) => {
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
  };

  // moveToNextCell - 훅의 함수를 사용하고 포커스 추가
  const handleMoveToNextCell = useCallback(() => {
    moveToNextCell();
    // 훅이 selectedCell을 업데이트하면 useEffect에서 포커스 처리
  }, [moveToNextCell]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent, row: number, col: number) => {
    if (e.key === ' ') {
      // 스페이스바: 다음 칸으로 이동
      e.preventDefault();
      handleMoveToNextCell();
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
  }, [handleMoveToNextCell, moveToPrevCell, handleArrowKey, updateUserInput]);

  const isCorrect = useCallback((row: number, col: number): boolean | null => {
    return checkCellCorrect(userInputs[row][col], grid[row][col].letter);
  }, [userInputs, grid]);

  const checkCellHighlighted = useCallback((row: number, col: number): boolean => {
    return isCellInSelectedWord(row, col, selectedWord);
  }, [selectedWord]);

  // 힌트 클릭 시 해당 단어의 첫 셀 선택
  useEffect(() => {
    if (selectedWordFromHint) {
      selectWordFromHint(selectedWordFromHint);
      inputRefs.current[selectedWordFromHint.startRow][selectedWordFromHint.startCol]?.focus();
    }
  }, [selectedWordFromHint, selectWordFromHint]);

  // 선택된 셀 변경 시 포커스 (훅에서 셀 변경 시)
  useEffect(() => {
    if (selectedCell) {
      inputRefs.current[selectedCell.row][selectedCell.col]?.focus();
    }
  }, [selectedCell]);

  // 그리드에서 단어 선택 시 부모에게 알림
  useEffect(() => {
    if (onWordSelect && selectedWord) {
      onWordSelect(selectedWord);
    }
  }, [selectedWord, onWordSelect]);

  // 완료 체크
  useEffect(() => {
    if (checkPuzzleCompletion(userInputs, grid)) {
      onComplete();
    }
  }, [userInputs, grid, onComplete]);

  return (
    <div className="puzzle-grid" style={{ gridTemplateColumns: `repeat(${gridSize}, 40px)` }}>
      {grid.map((row, rowIndex) =>
        row.map((cell, colIndex) => (
          <div
            key={`${rowIndex}-${colIndex}`}
            className={`puzzle-cell ${cell.isBlank ? 'blank' : 'active'}
              ${selectedCell?.row === rowIndex && selectedCell?.col === colIndex ? 'selected' : ''}
              ${checkCellHighlighted(rowIndex, colIndex) ? 'highlighted' : ''}
              ${isCorrect(rowIndex, colIndex) === true ? 'correct' : ''}
              ${isCorrect(rowIndex, colIndex) === false ? 'incorrect' : ''}`}
            onClick={() => handleCellClick(rowIndex, colIndex)}
          >
            {cell.acrossNumber && (
              <span
                className="word-number across-number"
                onClick={(e) => handleNumberClick(e, rowIndex, colIndex, 'ACROSS')}
              >
                {cell.acrossNumber}
              </span>
            )}
            {cell.downNumber && (
              <span
                className="word-number down-number"
                onClick={(e) => handleNumberClick(e, rowIndex, colIndex, 'DOWN')}
              >
                {cell.downNumber}
              </span>
            )}
            {!cell.isBlank && (
              <input
                ref={(el) => { inputRefs.current[rowIndex][colIndex] = el; }}
                type="text"
                onChange={(e) => handleInput(rowIndex, colIndex, e)}
                onKeyDown={(e) => handleKeyDown(e, rowIndex, colIndex)}
                onCompositionStart={handleCompositionStart}
                onCompositionEnd={(e) => handleCompositionEnd(rowIndex, colIndex, e)}
              />
            )}
          </div>
        ))
      )}
    </div>
  );
}

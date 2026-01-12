import { useRef, useEffect, useCallback } from 'react';
import type { PuzzleCell, PuzzleWord } from '../types/puzzle';
import {
  isCellInSelectedWord,
  checkCellCorrect,
  checkPuzzleCompletion
} from '../utils/puzzleUtils';
import { usePuzzleNavigation } from '../hooks/usePuzzleNavigation';
import { usePuzzleInput } from '../hooks/usePuzzleInput';
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
  const inputRefs = useRef<(HTMLInputElement | null)[][]>(
    Array(gridSize).fill(null).map(() => Array(gridSize).fill(null))
  );

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

  // 입력 훅 사용
  const {
    userInputs,
    handleInput,
    handleCompositionStart,
    handleCompositionEnd,
    handleKeyDown
  } = usePuzzleInput({
    grid,
    gridSize,
    selectedWord,
    selectedCell,
    setSelectedCell,
    inputRefs,
    moveToNextCell,
    moveToPrevCell,
    handleArrowKey
  });

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

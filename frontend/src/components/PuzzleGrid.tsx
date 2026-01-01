import { useState, useRef, useEffect } from 'react';
import type { PuzzleCell, PuzzleWord } from '../types/puzzle';
import './PuzzleGrid.css';

interface Props {
  grid: PuzzleCell[][];
  gridSize: number;
  acrossWords: PuzzleWord[];
  downWords: PuzzleWord[];
  onComplete: () => void;
  selectedWordFromHint?: PuzzleWord | null;
}

export default function PuzzleGrid({ grid, gridSize, acrossWords, downWords, onComplete, selectedWordFromHint }: Props) {
  const [userInputs, setUserInputs] = useState<string[][]>(() =>
    Array(gridSize).fill(null).map(() => Array(gridSize).fill(''))
  );
  const [selectedCell, setSelectedCell] = useState<{ row: number; col: number } | null>(null);
  const [selectedWord, setSelectedWord] = useState<PuzzleWord | null>(null);
  const inputRefs = useRef<(HTMLInputElement | null)[][]>(
    Array(gridSize).fill(null).map(() => Array(gridSize).fill(null))
  );
  const isComposing = useRef(false); // 한글 조합 중인지 추적

  const handleCellClick = (row: number, col: number) => {
    if (grid[row][col].isBlank) return;

    // 같은 셀을 다시 클릭했는지 확인
    const isSameCell = selectedCell?.row === row && selectedCell?.col === col;

    setSelectedCell({ row, col });
    inputRefs.current[row][col]?.focus();

    // 해당 셀이 속한 단어 찾기 (같은 셀이면 방향 전환)
    const word = findWordAtCell(row, col, isSameCell ? selectedWord?.direction : undefined);
    setSelectedWord(word);
  };

  const handleNumberClick = (e: React.MouseEvent, row: number, col: number, direction: 'ACROSS' | 'DOWN') => {
    e.stopPropagation(); // 셀 클릭 이벤트 전파 방지

    // 해당 방향의 단어 찾기
    const words = direction === 'ACROSS' ? acrossWords : downWords;
    const word = words.find(w => w.startRow === row && w.startCol === col);

    if (word) {
      setSelectedCell({ row, col });
      setSelectedWord(word);
      inputRefs.current[row][col]?.focus();
    }
  };

  const findWordAtCell = (row: number, col: number, currentDirection?: 'ACROSS' | 'DOWN'): PuzzleWord | null => {
    // 해당 셀에 속한 가로/세로 단어 찾기
    let acrossWord: PuzzleWord | null = null;
    let downWord: PuzzleWord | null = null;
    let isAcrossStart = false;
    let isDownStart = false;

    for (const word of acrossWords) {
      if (
        row === word.startRow &&
        col >= word.startCol &&
        col < word.startCol + word.word.length
      ) {
        acrossWord = word;
        isAcrossStart = (col === word.startCol);
        break;
      }
    }

    for (const word of downWords) {
      if (
        col === word.startCol &&
        row >= word.startRow &&
        row < word.startRow + word.word.length
      ) {
        downWord = word;
        isDownStart = (row === word.startRow);
        break;
      }
    }

    // 같은 셀 재클릭 시 방향 전환
    if (currentDirection === 'ACROSS' && downWord) {
      return downWord;
    }
    if (currentDirection === 'DOWN' && acrossWord) {
      return acrossWord;
    }

    // 해당 셀에서 시작하는 단어 우선 (한쪽만 시작점인 경우)
    if (isDownStart && !isAcrossStart && downWord) {
      return downWord;
    }
    if (isAcrossStart && !isDownStart && acrossWord) {
      return acrossWord;
    }

    // 기본: 가로 우선, 없으면 세로
    return acrossWord || downWord;
  };

  const updateUserInput = (row: number, col: number, value: string) => {
    setUserInputs(prev => {
      const newInputs = prev.map(r => [...r]); // 깊은 복사
      newInputs[row][col] = value;
      return newInputs;
    });
  };

  const handleInput = (row: number, col: number, e: React.ChangeEvent<HTMLInputElement>) => {
    if (grid[row][col].isBlank) return;

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

  const getNextCell = (row: number, col: number): { row: number; col: number } | null => {
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
  };

  const handleCompositionStart = () => {
    isComposing.current = true;
  };

  const handleCompositionEnd = (row: number, col: number, e: React.CompositionEvent<HTMLInputElement>) => {
    isComposing.current = false;
    const value = e.currentTarget.value;

    // 1글자만 유지
    if (value.length >= 1) {
      const firstChar = value.charAt(0);
      updateUserInput(row, col, firstChar);
      e.currentTarget.value = firstChar;
    }
  };

  const moveToNextCell = (row: number, col: number) => {
    if (!selectedWord) return;

    let nextRow = row;
    let nextCol = col;

    if (selectedWord.direction === 'ACROSS') {
      nextCol = col + 1;
      // 단어 범위 체크
      if (nextCol >= selectedWord.startCol + selectedWord.word.length) {
        return; // 단어 끝에 도달하면 이동하지 않음
      }
    } else {
      nextRow = row + 1;
      // 단어 범위 체크
      if (nextRow >= selectedWord.startRow + selectedWord.word.length) {
        return; // 단어 끝에 도달하면 이동하지 않음
      }
    }

    if (nextRow < gridSize && nextCol < gridSize && !grid[nextRow][nextCol].isBlank) {
      setSelectedCell({ row: nextRow, col: nextCol });
      inputRefs.current[nextRow][nextCol]?.focus();
    }
  };

  const moveCellWithDirection = (newRow: number, newCol: number) => {
    // 현재 단어 방향을 유지하면서 셀 이동
    setSelectedCell({ row: newRow, col: newCol });
    inputRefs.current[newRow][newCol]?.focus();
    // selectedWord는 유지 (방향 전환 없음)
  };

  const handleKeyDown = (e: React.KeyboardEvent, row: number, col: number) => {
    if (e.key === ' ') {
      // 스페이스바: 다음 칸으로 이동
      e.preventDefault();
      moveToNextCell(row, col);
    } else if (e.key === 'Backspace') {
      const currentInput = inputRefs.current[row][col];
      const currentValue = currentInput?.value || '';

      if (!currentValue) {
        // 현재 칸이 비어있으면 이전 칸으로 이동
        if (selectedWord) {
          let prevRow = row;
          let prevCol = col;

          if (selectedWord.direction === 'ACROSS') {
            prevCol = col - 1;
          } else {
            prevRow = row - 1;
          }

          if (prevRow >= 0 && prevCol >= 0 && !grid[prevRow][prevCol].isBlank) {
            moveCellWithDirection(prevRow, prevCol);
          }
        }
      } else {
        // 현재 칸 내용 삭제 후 상태 업데이트
        setTimeout(() => {
          updateUserInput(row, col, currentInput?.value || '');
        }, 0);
      }
    } else if (e.key === 'ArrowUp' && row > 0 && !grid[row - 1][col].isBlank) {
      moveCellWithDirection(row - 1, col);
    } else if (e.key === 'ArrowDown' && row < gridSize - 1 && !grid[row + 1][col].isBlank) {
      moveCellWithDirection(row + 1, col);
    } else if (e.key === 'ArrowLeft' && col > 0 && !grid[row][col - 1].isBlank) {
      moveCellWithDirection(row, col - 1);
    } else if (e.key === 'ArrowRight' && col < gridSize - 1 && !grid[row][col + 1].isBlank) {
      moveCellWithDirection(row, col + 1);
    }
  };

  const isCorrect = (row: number, col: number): boolean | null => {
    const inputValue = userInputs[row][col];
    if (!inputValue) return null;
    return inputValue === grid[row][col].letter;
  };

  const isCellInSelectedWord = (row: number, col: number): boolean => {
    if (!selectedWord) return false;

    if (selectedWord.direction === 'ACROSS') {
      return (
        row === selectedWord.startRow &&
        col >= selectedWord.startCol &&
        col < selectedWord.startCol + selectedWord.word.length
      );
    } else {
      return (
        col === selectedWord.startCol &&
        row >= selectedWord.startRow &&
        row < selectedWord.startRow + selectedWord.word.length
      );
    }
  };

  // 힌트 클릭 시 해당 단어의 첫 셀 선택
  // Note: This effect synchronizes external prop to local state - will be refactored in Phase 5
  useEffect(() => {
    if (selectedWordFromHint) {
      const { startRow, startCol } = selectedWordFromHint;
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSelectedCell({ row: startRow, col: startCol });
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSelectedWord(selectedWordFromHint);
      inputRefs.current[startRow][startCol]?.focus();
    }
  }, [selectedWordFromHint]);

  // 완료 체크
  useEffect(() => {
    let allCorrect = true;
    for (let row = 0; row < gridSize; row++) {
      for (let col = 0; col < gridSize; col++) {
        if (!grid[row][col].isBlank) {
          if (userInputs[row][col] !== grid[row][col].letter) {
            allCorrect = false;
            break;
          }
        }
      }
      if (!allCorrect) break;
    }
    if (allCorrect && userInputs.some(row => row.some(cell => cell !== ''))) {
      onComplete();
    }
  }, [userInputs, grid, gridSize, onComplete]);

  return (
    <div className="puzzle-grid" style={{ gridTemplateColumns: `repeat(${gridSize}, 40px)` }}>
      {grid.map((row, rowIndex) =>
        row.map((cell, colIndex) => (
          <div
            key={`${rowIndex}-${colIndex}`}
            className={`puzzle-cell ${cell.isBlank ? 'blank' : 'active'}
              ${selectedCell?.row === rowIndex && selectedCell?.col === colIndex ? 'selected' : ''}
              ${isCellInSelectedWord(rowIndex, colIndex) ? 'highlighted' : ''}
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

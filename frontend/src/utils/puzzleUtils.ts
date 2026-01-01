/**
 * 퍼즐 관련 유틸리티 함수
 */
import type { PuzzleWord, PuzzleCell } from '../types/puzzle';

/**
 * 특정 셀 위치에서 해당하는 단어를 찾습니다.
 *
 * @param row 행 인덱스
 * @param col 열 인덱스
 * @param acrossWords 가로 단어 목록
 * @param downWords 세로 단어 목록
 * @param currentDirection 현재 선택된 방향 (같은 셀 재클릭 시 방향 전환용)
 * @returns 찾은 단어 또는 null
 */
export function findWordAtCell(
  row: number,
  col: number,
  acrossWords: PuzzleWord[],
  downWords: PuzzleWord[],
  currentDirection?: 'ACROSS' | 'DOWN'
): PuzzleWord | null {
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
}

/**
 * 셀이 선택된 단어에 포함되는지 확인합니다.
 *
 * @param row 행 인덱스
 * @param col 열 인덱스
 * @param selectedWord 선택된 단어
 * @returns 포함 여부
 */
export function isCellInSelectedWord(
  row: number,
  col: number,
  selectedWord: PuzzleWord | null
): boolean {
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
}

/**
 * 셀의 입력값이 정답과 일치하는지 확인합니다.
 *
 * @param userInput 사용자 입력값
 * @param answer 정답 (null이면 빈 셀)
 * @returns true(정답), false(오답), null(미입력 또는 빈 셀)
 */
export function checkCellCorrect(
  userInput: string,
  answer: string | null
): boolean | null {
  if (answer === null) return null;
  const trimmedInput = userInput.trim();
  if (!trimmedInput) return null;
  return trimmedInput === answer;
}

/**
 * 퍼즐이 완료되었는지 확인합니다.
 * 모든 비-블랭크 셀이 정답과 일치하면 완료입니다.
 *
 * @param userInputs 사용자 입력 배열
 * @param grid 퍼즐 그리드
 * @returns 완료 여부
 */
export function checkPuzzleCompletion(
  userInputs: string[][],
  grid: PuzzleCell[][]
): boolean {
  let hasAnyInput = false;

  for (let row = 0; row < grid.length; row++) {
    for (let col = 0; col < grid[row].length; col++) {
      const cell = grid[row][col];
      if (!cell.isBlank && cell.letter !== null) {
        const userInput = userInputs[row]?.[col] ?? '';
        if (userInput) {
          hasAnyInput = true;
        }
        if (userInput !== cell.letter) {
          return false;
        }
      }
    }
  }

  // 최소 하나의 입력이 있어야 완료 처리
  return hasAnyInput;
}

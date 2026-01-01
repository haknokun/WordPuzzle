import { describe, it, expect } from 'vitest';
import {
  findWordAtCell,
  isCellInSelectedWord,
  checkCellCorrect,
  checkPuzzleCompletion
} from '../../utils/puzzleUtils';
import type { PuzzleWord, PuzzleCell } from '../../types/puzzle';

// 테스트용 Mock 데이터
const mockAcrossWords: PuzzleWord[] = [
  { number: 1, word: '사과', definition: '과일', startRow: 0, startCol: 0, direction: 'ACROSS' },
  { number: 2, word: '바나나', definition: '열대과일', startRow: 2, startCol: 1, direction: 'ACROSS' }
];

const mockDownWords: PuzzleWord[] = [
  { number: 1, word: '사람', definition: '인간', startRow: 0, startCol: 0, direction: 'DOWN' },
  { number: 2, word: '과자', definition: '간식', startRow: 0, startCol: 1, direction: 'DOWN' }
];

// 3x5 그리드 Mock
const mockGrid: PuzzleCell[][] = [
  // row 0: 사 과 _ _ _
  [
    { row: 0, col: 0, letter: '사', isBlank: false, acrossNumber: 1, downNumber: 1 },
    { row: 0, col: 1, letter: '과', isBlank: false, acrossNumber: null, downNumber: 2 },
    { row: 0, col: 2, letter: null, isBlank: true, acrossNumber: null, downNumber: null },
    { row: 0, col: 3, letter: null, isBlank: true, acrossNumber: null, downNumber: null },
    { row: 0, col: 4, letter: null, isBlank: true, acrossNumber: null, downNumber: null }
  ],
  // row 1: 람 자 _ _ _
  [
    { row: 1, col: 0, letter: '람', isBlank: false, acrossNumber: null, downNumber: null },
    { row: 1, col: 1, letter: '자', isBlank: false, acrossNumber: null, downNumber: null },
    { row: 1, col: 2, letter: null, isBlank: true, acrossNumber: null, downNumber: null },
    { row: 1, col: 3, letter: null, isBlank: true, acrossNumber: null, downNumber: null },
    { row: 1, col: 4, letter: null, isBlank: true, acrossNumber: null, downNumber: null }
  ],
  // row 2: _ 바 나 나 _
  [
    { row: 2, col: 0, letter: null, isBlank: true, acrossNumber: null, downNumber: null },
    { row: 2, col: 1, letter: '바', isBlank: false, acrossNumber: 2, downNumber: null },
    { row: 2, col: 2, letter: '나', isBlank: false, acrossNumber: null, downNumber: null },
    { row: 2, col: 3, letter: '나', isBlank: false, acrossNumber: null, downNumber: null },
    { row: 2, col: 4, letter: null, isBlank: true, acrossNumber: null, downNumber: null }
  ]
];

describe('puzzleUtils', () => {
  describe('findWordAtCell', () => {
    it('가로 단어의 셀을 클릭하면 가로 단어를 반환', () => {
      const result = findWordAtCell(0, 1, mockAcrossWords, mockDownWords);
      // (0,1)은 가로 '사과'의 두번째 글자이면서 세로 '과자'의 시작점
      // 시작점인 세로 단어가 우선
      expect(result).not.toBeNull();
    });

    it('세로 단어만 있는 셀에서 세로 단어 반환', () => {
      const result = findWordAtCell(1, 0, mockAcrossWords, mockDownWords);
      expect(result).not.toBeNull();
      expect(result?.direction).toBe('DOWN');
      expect(result?.word).toBe('사람');
    });

    it('가로 단어만 있는 셀에서 가로 단어 반환', () => {
      const result = findWordAtCell(2, 2, mockAcrossWords, mockDownWords);
      expect(result).not.toBeNull();
      expect(result?.direction).toBe('ACROSS');
      expect(result?.word).toBe('바나나');
    });

    it('교차점에서 현재 방향이 ACROSS면 DOWN으로 전환', () => {
      // (0,0)은 '사과'와 '사람'의 교차점
      const result = findWordAtCell(0, 0, mockAcrossWords, mockDownWords, 'ACROSS');
      expect(result?.direction).toBe('DOWN');
    });

    it('교차점에서 현재 방향이 DOWN이면 ACROSS로 전환', () => {
      const result = findWordAtCell(0, 0, mockAcrossWords, mockDownWords, 'DOWN');
      expect(result?.direction).toBe('ACROSS');
    });

    it('단어 범위 밖 셀에서는 null 반환', () => {
      const result = findWordAtCell(0, 4, mockAcrossWords, mockDownWords);
      expect(result).toBeNull();
    });
  });

  describe('isCellInSelectedWord', () => {
    const acrossWord = mockAcrossWords[0]; // '사과' at (0,0)
    const downWord = mockDownWords[0];     // '사람' at (0,0)

    it('가로 단어에 포함된 셀이면 true', () => {
      expect(isCellInSelectedWord(0, 0, acrossWord)).toBe(true);
      expect(isCellInSelectedWord(0, 1, acrossWord)).toBe(true);
    });

    it('가로 단어에 포함되지 않은 셀이면 false', () => {
      expect(isCellInSelectedWord(1, 0, acrossWord)).toBe(false);
      expect(isCellInSelectedWord(0, 2, acrossWord)).toBe(false);
    });

    it('세로 단어에 포함된 셀이면 true', () => {
      expect(isCellInSelectedWord(0, 0, downWord)).toBe(true);
      expect(isCellInSelectedWord(1, 0, downWord)).toBe(true);
    });

    it('세로 단어에 포함되지 않은 셀이면 false', () => {
      expect(isCellInSelectedWord(0, 1, downWord)).toBe(false);
      expect(isCellInSelectedWord(2, 0, downWord)).toBe(false);
    });

    it('selectedWord가 null이면 false', () => {
      expect(isCellInSelectedWord(0, 0, null)).toBe(false);
    });
  });

  describe('checkCellCorrect', () => {
    it('입력값이 정답과 같으면 true', () => {
      expect(checkCellCorrect('사', '사')).toBe(true);
      expect(checkCellCorrect('과', '과')).toBe(true);
    });

    it('입력값이 정답과 다르면 false', () => {
      expect(checkCellCorrect('가', '사')).toBe(false);
      expect(checkCellCorrect('나', '과')).toBe(false);
    });

    it('입력값이 빈 문자열이면 null', () => {
      expect(checkCellCorrect('', '사')).toBeNull();
    });

    it('입력값이 공백이면 null', () => {
      expect(checkCellCorrect(' ', '사')).toBeNull();
    });

    it('정답이 null이면 null', () => {
      expect(checkCellCorrect('사', null)).toBeNull();
      expect(checkCellCorrect('', null)).toBeNull();
    });
  });

  describe('checkPuzzleCompletion', () => {
    it('모든 셀이 정답이면 true', () => {
      const userInputs = [
        ['사', '과', '', '', ''],
        ['람', '자', '', '', ''],
        ['', '바', '나', '나', '']
      ];
      expect(checkPuzzleCompletion(userInputs, mockGrid)).toBe(true);
    });

    it('하나라도 틀리면 false', () => {
      const userInputs = [
        ['가', '과', '', '', ''],  // '가' instead of '사'
        ['람', '자', '', '', ''],
        ['', '바', '나', '나', '']
      ];
      expect(checkPuzzleCompletion(userInputs, mockGrid)).toBe(false);
    });

    it('빈 셀이 있으면 false', () => {
      const userInputs = [
        ['사', '', '', '', ''],   // '과' is empty
        ['람', '자', '', '', ''],
        ['', '바', '나', '나', '']
      ];
      expect(checkPuzzleCompletion(userInputs, mockGrid)).toBe(false);
    });

    it('모든 입력이 비어있으면 false', () => {
      const userInputs = [
        ['', '', '', '', ''],
        ['', '', '', '', ''],
        ['', '', '', '', '']
      ];
      expect(checkPuzzleCompletion(userInputs, mockGrid)).toBe(false);
    });

    it('isBlank 셀은 검사하지 않음', () => {
      // isBlank 셀에 아무 값이나 있어도 상관없음
      const userInputs = [
        ['사', '과', 'X', 'Y', 'Z'],  // blank cells have values
        ['람', '자', 'A', 'B', 'C'],
        ['Q', '바', '나', '나', 'W']
      ];
      expect(checkPuzzleCompletion(userInputs, mockGrid)).toBe(true);
    });
  });
});

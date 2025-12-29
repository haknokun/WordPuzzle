export interface PuzzleCell {
  row: number;
  col: number;
  letter: string | null;
  isBlank: boolean;
  acrossNumber: number | null;  // 가로 단어 시작 번호
  downNumber: number | null;    // 세로 단어 시작 번호
}

export interface PuzzleWord {
  number: number;
  word: string;
  definition: string;
  startRow: number;
  startCol: number;
  direction: 'ACROSS' | 'DOWN';
}

export interface PuzzleResponse {
  gridSize: number;
  grid: PuzzleCell[][];
  acrossWords: PuzzleWord[];
  downWords: PuzzleWord[];
  totalWords: number;
}

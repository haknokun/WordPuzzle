import type { PuzzleResponse } from '../types/puzzle';

const API_BASE = 'http://localhost:8080/api';

export const generatePuzzle = async (
  wordCount: number = 10,
  level?: string
): Promise<PuzzleResponse> => {
  let url = `${API_BASE}/puzzle/generate?wordCount=${wordCount}`;
  if (level) {
    url += `&level=${encodeURIComponent(level)}`;
  }
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error('퍼즐 생성 실패');
  }
  return response.json();
};

export const importData = async (path: string): Promise<{ success: boolean; message: string }> => {
  const response = await fetch(`${API_BASE}/import?path=${encodeURIComponent(path)}`, {
    method: 'POST',
  });
  return response.json();
};

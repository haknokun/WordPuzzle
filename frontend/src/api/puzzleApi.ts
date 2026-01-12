import type { PuzzleResponse } from '../types/puzzle';

const API_BASE = 'http://localhost:8080/api';

export interface GeneratePuzzleOptions {
  wordCount?: number;
  level?: string;
  source?: 'default' | 'std';
  category?: string;
  wordType?: string;
}

export const generatePuzzle = async (
  wordCount: number = 10,
  level?: string,
  source: 'default' | 'std' = 'std',
  category?: string,
  wordType?: string
): Promise<PuzzleResponse> => {
  const params = new URLSearchParams();
  params.append('wordCount', String(wordCount));
  params.append('source', source);

  if (level) {
    params.append('level', level);
  }
  if (category) {
    params.append('category', category);
  }
  if (wordType) {
    params.append('wordType', wordType);
  }

  const response = await fetch(`${API_BASE}/puzzle/generate?${params.toString()}`);
  if (!response.ok) {
    throw new Error('퍼즐 생성 실패');
  }
  return response.json();
};

export const fetchCategories = async (): Promise<string[]> => {
  const response = await fetch(`${API_BASE}/categories`);
  if (!response.ok) {
    throw new Error('카테고리 목록 조회 실패');
  }
  return response.json();
};

export const fetchWordTypes = async (): Promise<{ code: string; name: string; description: string }[]> => {
  const response = await fetch(`${API_BASE}/word-types`);
  if (!response.ok) {
    throw new Error('단어 유형 목록 조회 실패');
  }
  return response.json();
};

export const importData = async (path: string): Promise<{ success: boolean; message: string }> => {
  const response = await fetch(`${API_BASE}/import?path=${encodeURIComponent(path)}`, {
    method: 'POST',
  });
  return response.json();
};

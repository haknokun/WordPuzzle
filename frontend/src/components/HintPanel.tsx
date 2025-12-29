import { useState } from 'react';
import type { PuzzleWord } from '../types/puzzle';
import './HintPanel.css';

interface Props {
  acrossWords: PuzzleWord[];
  downWords: PuzzleWord[];
  selectedWord: PuzzleWord | null;
  onWordClick: (word: PuzzleWord) => void;
}

// 한글 초성 추출 함수
const CHOSUNG = ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'];

function getChosung(word: string): string {
  return word.split('').map(char => {
    const code = char.charCodeAt(0);
    // 한글 범위: 0xAC00 ~ 0xD7A3
    if (code >= 0xAC00 && code <= 0xD7A3) {
      const chosungIndex = Math.floor((code - 0xAC00) / 588);
      return CHOSUNG[chosungIndex];
    }
    return char; // 한글이 아니면 그대로 반환
  }).join('');
}

export default function HintPanel({ acrossWords, downWords, selectedWord, onWordClick }: Props) {
  const [revealedChosung, setRevealedChosung] = useState<Set<string>>(new Set());

  const toggleChosung = (e: React.MouseEvent, key: string) => {
    e.stopPropagation(); // 힌트 클릭 이벤트 전파 방지
    setRevealedChosung(prev => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  return (
    <div className="hint-panel">
      <div className="hint-section">
        <h3>가로 열쇠</h3>
        <ul>
          {acrossWords.map((word) => {
            const key = `across-${word.number}`;
            const isRevealed = revealedChosung.has(key);
            return (
              <li
                key={key}
                className={selectedWord?.number === word.number && selectedWord?.direction === 'ACROSS' ? 'selected' : ''}
                onClick={() => onWordClick(word)}
              >
                <div className="hint-content">
                  <span className="hint-number">{word.number}.</span>
                  <span className="hint-text">{word.definition}</span>
                </div>
                <div className="hint-actions">
                  {isRevealed && <span className="chosung-text">{getChosung(word.word)}</span>}
                  <button
                    className={`chosung-btn ${isRevealed ? 'active' : ''}`}
                    onClick={(e) => toggleChosung(e, key)}
                    title="초성 힌트"
                  >
                    ㄱ
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      </div>

      <div className="hint-section">
        <h3>세로 열쇠</h3>
        <ul>
          {downWords.map((word) => {
            const key = `down-${word.number}`;
            const isRevealed = revealedChosung.has(key);
            return (
              <li
                key={key}
                className={selectedWord?.number === word.number && selectedWord?.direction === 'DOWN' ? 'selected' : ''}
                onClick={() => onWordClick(word)}
              >
                <div className="hint-content">
                  <span className="hint-number">{word.number}.</span>
                  <span className="hint-text">{word.definition}</span>
                </div>
                <div className="hint-actions">
                  {isRevealed && <span className="chosung-text">{getChosung(word.word)}</span>}
                  <button
                    className={`chosung-btn ${isRevealed ? 'active' : ''}`}
                    onClick={(e) => toggleChosung(e, key)}
                    title="초성 힌트"
                  >
                    ㄱ
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}

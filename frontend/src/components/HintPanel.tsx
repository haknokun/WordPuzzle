import { useState } from 'react';
import type { PuzzleWord } from '../types/puzzle';
import { getChosung } from '../utils/chosung';
import './HintPanel.css';

interface Props {
  acrossWords: PuzzleWord[];
  downWords: PuzzleWord[];
  selectedWord: PuzzleWord | null;
  onWordClick: (word: PuzzleWord) => void;
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

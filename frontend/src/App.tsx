import { useState, useCallback } from 'react';
import type { PuzzleResponse, PuzzleWord } from './types/puzzle';
import { generatePuzzle } from './api/puzzleApi';
import PuzzleGrid from './components/PuzzleGrid';
import HintPanel from './components/HintPanel';
import ThemeSelector from './components/ThemeSelector';
import './App.css';

function App() {
  const [puzzle, setPuzzle] = useState<PuzzleResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedWord, setSelectedWord] = useState<PuzzleWord | null>(null);
  const [completed, setCompleted] = useState(false);
  const [wordCount, setWordCount] = useState(10);
  const [level, setLevel] = useState<string>('');
  const [source, setSource] = useState<'default' | 'std'>('std');
  const [category, setCategory] = useState<string | null>(null);
  const [wordType, setWordType] = useState<string | null>(null);

  const handleGenerate = async () => {
    setLoading(true);
    setError(null);
    setCompleted(false);
    try {
      const data = await generatePuzzle(
        wordCount,
        level || undefined,
        source,
        category || undefined,
        wordType || undefined
      );
      console.log('API Response:', data);
      console.log('Grid:', data.grid);
      setPuzzle(data);
    } catch {
      setError('퍼즐 생성에 실패했습니다. 서버가 실행 중인지 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  const handleWordClick = (word: PuzzleWord) => {
    // 같은 힌트를 다시 클릭해도 동작하도록 새 객체 생성
    setSelectedWord({ ...word });
  };

  const handleWordSelect = useCallback((word: PuzzleWord | null) => {
    setSelectedWord(word);
  }, []);

  const handleComplete = useCallback(() => {
    setCompleted(true);
  }, []);

  return (
    <div className="app">
      <header>
        <h1>십자말풀이</h1>
      </header>

      <div className="controls">
        <label>
          단어 수:
          <input
            type="number"
            value={wordCount}
            onChange={(e) => setWordCount(Number(e.target.value))}
            min={3}
            max={50}
          />
        </label>
        <label>
          난이도:
          <select
            value={level}
            onChange={(e) => setLevel(e.target.value)}
            disabled={source === 'std'}
          >
            <option value="">전체</option>
            <option value="초급">초급</option>
            <option value="중급">중급</option>
            <option value="고급">고급</option>
          </select>
        </label>
        <label>
          데이터:
          <select value={source} onChange={(e) => setSource(e.target.value as 'default' | 'std')}>
            <option value="std">표준국어대사전</option>
            <option value="default">한국어기초사전</option>
          </select>
        </label>
        <button onClick={handleGenerate} disabled={loading}>
          {loading ? '생성 중...' : '새 퍼즐 생성'}
        </button>
      </div>

      {source === 'std' && (
        <div className="theme-controls">
          <ThemeSelector
            onCategoryChange={setCategory}
            onWordTypeChange={setWordType}
            disabled={loading}
          />
        </div>
      )}

      {error && <div className="error">{error}</div>}

      {completed && (
        <div className="complete-message">
          축하합니다! 퍼즐을 완성했습니다!
        </div>
      )}

      {puzzle && (
        <>
          <div className="puzzle-info">
            총 {puzzle.totalWords}개 단어 | {puzzle.gridSize}x{puzzle.gridSize} 그리드
          </div>
          <div className="puzzle-container">
            <div className="puzzle-left">
              <PuzzleGrid
                grid={puzzle.grid}
                gridSize={puzzle.gridSize}
                acrossWords={puzzle.acrossWords}
                downWords={puzzle.downWords}
                onComplete={handleComplete}
                selectedWordFromHint={selectedWord}
                onWordSelect={handleWordSelect}
              />
            </div>
            <div className="puzzle-right">
              <HintPanel
                acrossWords={puzzle.acrossWords}
                downWords={puzzle.downWords}
                selectedWord={selectedWord}
                onWordClick={handleWordClick}
              />
            </div>
          </div>
        </>
      )}

      {!puzzle && !loading && (
        <div className="welcome">
          <p>새 퍼즐 생성 버튼을 클릭하여 시작하세요!</p>
        </div>
      )}
    </div>
  );
}

export default App;

import { useState, useCallback } from 'react';
import type { PuzzleResponse, PuzzleWord } from './types/puzzle';
import { generatePuzzle } from './api/puzzleApi';
import PuzzleGrid from './components/PuzzleGrid';
import HintPanel from './components/HintPanel';
import './App.css';

function App() {
  const [puzzle, setPuzzle] = useState<PuzzleResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedWord, setSelectedWord] = useState<PuzzleWord | null>(null);
  const [completed, setCompleted] = useState(false);
  const [gridSize, setGridSize] = useState(15);
  const [wordCount, setWordCount] = useState(10);

  const handleGenerate = async () => {
    setLoading(true);
    setError(null);
    setCompleted(false);
    try {
      const data = await generatePuzzle(gridSize, wordCount);
      console.log('API Response:', data);
      console.log('Grid:', data.grid);
      setPuzzle(data);
    } catch (err) {
      setError('퍼즐 생성에 실패했습니다. 서버가 실행 중인지 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  const handleWordClick = (word: PuzzleWord) => {
    // 같은 힌트를 다시 클릭해도 동작하도록 새 객체 생성
    setSelectedWord({ ...word });
  };

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
          그리드 크기:
          <input
            type="number"
            value={gridSize}
            onChange={(e) => setGridSize(Number(e.target.value))}
            min={5}
            max={30}
          />
        </label>
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
        <button onClick={handleGenerate} disabled={loading}>
          {loading ? '생성 중...' : '새 퍼즐 생성'}
        </button>
      </div>

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

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import HintPanel from '../../components/HintPanel';
import type { PuzzleWord } from '../../types/puzzle';

const mockAcrossWords: PuzzleWord[] = [
  { number: 1, word: '사과', definition: '빨간 과일', startRow: 0, startCol: 0, direction: 'ACROSS' },
  { number: 2, word: '바나나', definition: '노란 열대과일', startRow: 2, startCol: 1, direction: 'ACROSS' }
];

const mockDownWords: PuzzleWord[] = [
  { number: 1, word: '사람', definition: '인간', startRow: 0, startCol: 0, direction: 'DOWN' },
  { number: 2, word: '과자', definition: '달콤한 간식', startRow: 0, startCol: 1, direction: 'DOWN' }
];

describe('HintPanel', () => {
  describe('렌더링', () => {
    it('가로 열쇠 섹션이 렌더링됨', () => {
      render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          selectedWord={null}
          onWordClick={vi.fn()}
        />
      );

      expect(screen.getByText('가로 열쇠')).toBeInTheDocument();
    });

    it('세로 열쇠 섹션이 렌더링됨', () => {
      render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          selectedWord={null}
          onWordClick={vi.fn()}
        />
      );

      expect(screen.getByText('세로 열쇠')).toBeInTheDocument();
    });

    it('가로 단어 힌트들이 렌더링됨', () => {
      render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          selectedWord={null}
          onWordClick={vi.fn()}
        />
      );

      expect(screen.getByText('빨간 과일')).toBeInTheDocument();
      expect(screen.getByText('노란 열대과일')).toBeInTheDocument();
    });

    it('세로 단어 힌트들이 렌더링됨', () => {
      render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          selectedWord={null}
          onWordClick={vi.fn()}
        />
      );

      expect(screen.getByText('인간')).toBeInTheDocument();
      expect(screen.getByText('달콤한 간식')).toBeInTheDocument();
    });

    it('단어 번호가 렌더링됨', () => {
      render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          selectedWord={null}
          onWordClick={vi.fn()}
        />
      );

      // 번호 1과 2가 각 섹션에 있어야 함
      const numberOnes = screen.getAllByText('1.');
      const numberTwos = screen.getAllByText('2.');

      expect(numberOnes.length).toBeGreaterThanOrEqual(2);
      expect(numberTwos.length).toBeGreaterThanOrEqual(2);
    });
  });

  describe('상호작용', () => {
    it('힌트 클릭 시 onWordClick이 호출됨', () => {
      const mockOnWordClick = vi.fn();

      render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          selectedWord={null}
          onWordClick={mockOnWordClick}
        />
      );

      fireEvent.click(screen.getByText('빨간 과일'));

      expect(mockOnWordClick).toHaveBeenCalledWith(mockAcrossWords[0]);
    });

    it('초성 버튼 클릭 시 초성이 표시됨', () => {
      render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          selectedWord={null}
          onWordClick={vi.fn()}
        />
      );

      // 초성 버튼들 찾기 (ㄱ 버튼)
      const chosungButtons = screen.getAllByTitle('초성 힌트');

      // 첫 번째 초성 버튼 클릭
      fireEvent.click(chosungButtons[0]);

      // '사과'의 초성 'ㅅㄱ'이 표시되어야 함
      expect(screen.getByText('ㅅㄱ')).toBeInTheDocument();
    });

    it('초성 버튼 재클릭 시 초성이 숨겨짐', () => {
      render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          selectedWord={null}
          onWordClick={vi.fn()}
        />
      );

      const chosungButtons = screen.getAllByTitle('초성 힌트');

      // 첫 번째 클릭 - 초성 표시
      fireEvent.click(chosungButtons[0]);
      expect(screen.getByText('ㅅㄱ')).toBeInTheDocument();

      // 두 번째 클릭 - 초성 숨김
      fireEvent.click(chosungButtons[0]);
      expect(screen.queryByText('ㅅㄱ')).not.toBeInTheDocument();
    });
  });

  describe('선택된 단어 하이라이팅', () => {
    it('선택된 가로 단어가 하이라이트됨', () => {
      const { container } = render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          selectedWord={mockAcrossWords[0]}
          onWordClick={vi.fn()}
        />
      );

      // 첫 번째 가로 단어가 선택됨
      const selectedItem = container.querySelector('li.selected');
      expect(selectedItem).toBeInTheDocument();
      expect(selectedItem).toHaveTextContent('빨간 과일');
    });

    it('선택된 세로 단어가 하이라이트됨', () => {
      const { container } = render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={mockDownWords}
          selectedWord={mockDownWords[1]}
          onWordClick={vi.fn()}
        />
      );

      const selectedItem = container.querySelector('li.selected');
      expect(selectedItem).toBeInTheDocument();
      expect(selectedItem).toHaveTextContent('달콤한 간식');
    });
  });

  describe('빈 단어 목록 처리', () => {
    it('가로 단어가 없어도 섹션은 렌더링됨', () => {
      render(
        <HintPanel
          acrossWords={[]}
          downWords={mockDownWords}
          selectedWord={null}
          onWordClick={vi.fn()}
        />
      );

      expect(screen.getByText('가로 열쇠')).toBeInTheDocument();
    });

    it('세로 단어가 없어도 섹션은 렌더링됨', () => {
      render(
        <HintPanel
          acrossWords={mockAcrossWords}
          downWords={[]}
          selectedWord={null}
          onWordClick={vi.fn()}
        />
      );

      expect(screen.getByText('세로 열쇠')).toBeInTheDocument();
    });
  });
});

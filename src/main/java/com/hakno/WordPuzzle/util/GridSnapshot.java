package com.hakno.WordPuzzle.util;

import com.hakno.WordPuzzle.dto.PuzzleWord;
import lombok.Getter;

import java.util.*;

/**
 * 백트래킹 알고리즘에서 그리드 상태 저장/복원을 위한 스냅샷 클래스
 */
@Getter
public class GridSnapshot {
    private final char[][] gridState;
    private final List<PuzzleWord> placedWords;
    private final Set<String> usedWords;
    private final int gridSize;

    public GridSnapshot(char[][] grid, List<PuzzleWord> placedWords, Set<String> usedWords) {
        this.gridSize = grid.length;
        this.gridState = copyGrid(grid);
        this.placedWords = new ArrayList<>(placedWords);
        this.usedWords = new HashSet<>(usedWords);
    }

    /**
     * 그리드 복사본 생성
     */
    private char[][] copyGrid(char[][] original) {
        int size = original.length;
        char[][] copy = new char[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, size);
        }
        return copy;
    }

    /**
     * 스냅샷으로부터 그리드 복원
     */
    public void restoreTo(char[][] targetGrid, List<PuzzleWord> targetWords, Set<String> targetUsedWords) {
        // 그리드 복원
        for (int i = 0; i < gridSize; i++) {
            System.arraycopy(gridState[i], 0, targetGrid[i], 0, gridSize);
        }

        // 배치된 단어 목록 복원
        targetWords.clear();
        targetWords.addAll(placedWords);

        // 사용된 단어 세트 복원
        targetUsedWords.clear();
        targetUsedWords.addAll(usedWords);
    }

    /**
     * 현재 배치된 단어 수
     */
    public int getPlacedWordCount() {
        return placedWords.size();
    }

    /**
     * 그리드의 채워진 셀 수
     */
    public int getFilledCellCount() {
        int count = 0;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (gridState[i][j] != '\0') {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 디버깅용 그리드 문자열
     */
    public String toGridString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                char c = gridState[i][j];
                sb.append(c == '\0' ? "□" : c);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("GridSnapshot[words=%d, cells=%d]",
            placedWords.size(), getFilledCellCount());
    }
}

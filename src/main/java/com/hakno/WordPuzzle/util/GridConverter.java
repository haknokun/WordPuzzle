package com.hakno.WordPuzzle.util;

import com.hakno.WordPuzzle.dto.PuzzleCell;
import com.hakno.WordPuzzle.dto.PuzzleWord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 그리드 변환 유틸리티
 * char[][] -> List<List<PuzzleCell>> 변환
 */
@Component
public class GridConverter {

    /**
     * char 2차원 배열을 PuzzleCell 리스트로 변환
     *
     * @param grid        char 그리드
     * @param acrossWords 가로 단어 목록
     * @param downWords   세로 단어 목록
     * @param gridSize    그리드 크기
     * @return PuzzleCell 2차원 리스트
     */
    public List<List<PuzzleCell>> convertToCellGrid(char[][] grid, List<PuzzleWord> acrossWords,
                                                      List<PuzzleWord> downWords, int gridSize) {
        // 시작점에 번호 매핑
        Map<String, Integer> acrossNumbers = buildNumberMap(acrossWords);
        Map<String, Integer> downNumbers = buildNumberMap(downWords);

        // 그리드 변환
        List<List<PuzzleCell>> cellGrid = new ArrayList<>();
        for (int row = 0; row < gridSize; row++) {
            List<PuzzleCell> rowCells = new ArrayList<>();
            for (int col = 0; col < gridSize; col++) {
                String key = row + "," + col;
                char c = grid[row][col];

                PuzzleCell cell = PuzzleCell.builder()
                        .row(row)
                        .col(col)
                        .letter(c == '\0' ? null : String.valueOf(c))
                        .isBlank(c == '\0')
                        .acrossNumber(acrossNumbers.get(key))
                        .downNumber(downNumbers.get(key))
                        .build();

                rowCells.add(cell);
            }
            cellGrid.add(rowCells);
        }
        return cellGrid;
    }

    private Map<String, Integer> buildNumberMap(List<PuzzleWord> words) {
        Map<String, Integer> numberMap = new HashMap<>();
        for (PuzzleWord word : words) {
            String key = word.getStartRow() + "," + word.getStartCol();
            numberMap.put(key, word.getNumber());
        }
        return numberMap;
    }
}

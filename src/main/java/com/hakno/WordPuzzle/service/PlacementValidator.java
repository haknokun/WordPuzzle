package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.dto.PuzzleWord;
import org.springframework.stereotype.Component;

/**
 * 단어 배치 검증 로직
 * PuzzleGeneratorService에서 추출된 순수 함수들
 */
@Component
public class PlacementValidator {

    /**
     * 주어진 위치에 단어를 배치할 수 있는지 검증
     *
     * @param grid      현재 그리드 상태
     * @param word      배치할 단어
     * @param startRow  시작 행
     * @param startCol  시작 열
     * @param direction 방향 (ACROSS/DOWN)
     * @param gridSize  그리드 크기
     * @return 배치 가능 여부
     */
    public boolean canPlaceWord(char[][] grid, String word, int startRow, int startCol,
                                PuzzleWord.Direction direction, int gridSize) {
        int len = word.length();

        if (direction == PuzzleWord.Direction.ACROSS) {
            return canPlaceAcross(grid, word, startRow, startCol, len, gridSize);
        } else {
            return canPlaceDown(grid, word, startRow, startCol, len, gridSize);
        }
    }

    private boolean canPlaceAcross(char[][] grid, String word, int startRow, int startCol, int len, int gridSize) {
        // 범위 체크
        if (startCol < 0 || startCol + len > gridSize) return false;
        if (startRow < 0 || startRow >= gridSize) return false;

        // 단어 앞뒤에 빈 칸 확보
        if (startCol > 0 && grid[startRow][startCol - 1] != '\0') return false;
        if (startCol + len < gridSize && grid[startRow][startCol + len] != '\0') return false;

        boolean hasIntersection = false;
        for (int i = 0; i < len; i++) {
            int col = startCol + i;
            char existing = grid[startRow][col];
            char newChar = word.charAt(i);

            if (existing != '\0') {
                // 교차점: 같은 글자여야 함
                if (existing != newChar) return false;
                hasIntersection = true;
            } else {
                // 빈 셀: 교차점이 아니면 위/아래에 글자가 있으면 안됨 (단어 분리)
                boolean hasAbove = startRow > 0 && grid[startRow - 1][col] != '\0';
                boolean hasBelow = startRow < gridSize - 1 && grid[startRow + 1][col] != '\0';

                if (hasAbove || hasBelow) {
                    return false;
                }
            }
        }
        return hasIntersection;
    }

    private boolean canPlaceDown(char[][] grid, String word, int startRow, int startCol, int len, int gridSize) {
        // 범위 체크
        if (startRow < 0 || startRow + len > gridSize) return false;
        if (startCol < 0 || startCol >= gridSize) return false;

        // 단어 앞뒤에 빈 칸 확보
        if (startRow > 0 && grid[startRow - 1][startCol] != '\0') return false;
        if (startRow + len < gridSize && grid[startRow + len][startCol] != '\0') return false;

        boolean hasIntersection = false;
        for (int i = 0; i < len; i++) {
            int row = startRow + i;
            char existing = grid[row][startCol];
            char newChar = word.charAt(i);

            if (existing != '\0') {
                // 교차점: 같은 글자여야 함
                if (existing != newChar) return false;
                hasIntersection = true;
            } else {
                // 빈 셀: 교차점이 아니면 좌/우에 글자가 있으면 안됨 (단어 분리)
                boolean hasLeft = startCol > 0 && grid[row][startCol - 1] != '\0';
                boolean hasRight = startCol < gridSize - 1 && grid[row][startCol + 1] != '\0';

                if (hasLeft || hasRight) {
                    return false;
                }
            }
        }
        return hasIntersection;
    }

    /**
     * 배치가 불가능한 이유를 반환 (디버깅용)
     */
    public String whyCannotPlace(char[][] grid, String word, int startRow, int startCol,
                                  PuzzleWord.Direction direction, int gridSize) {
        int len = word.length();

        if (direction == PuzzleWord.Direction.ACROSS) {
            if (startCol < 0) return "시작열 음수";
            if (startCol + len > gridSize) return "범위초과(열)";
            if (startRow < 0 || startRow >= gridSize) return "행 범위 초과";
            if (startCol > 0 && grid[startRow][startCol - 1] != '\0') return "앞에 글자있음";
            if (startCol + len < gridSize && grid[startRow][startCol + len] != '\0') return "뒤에 글자있음";

            boolean hasIntersection = false;
            for (int i = 0; i < len; i++) {
                int col = startCol + i;
                char existing = grid[startRow][col];
                char newChar = word.charAt(i);

                if (existing != '\0') {
                    if (existing != newChar) return "교차점 글자불일치: " + existing + "!=" + newChar;
                    hasIntersection = true;
                } else {
                    boolean hasAbove = startRow > 0 && grid[startRow - 1][col] != '\0';
                    boolean hasBelow = startRow < gridSize - 1 && grid[startRow + 1][col] != '\0';
                    if (hasAbove) return "위에 인접글자";
                    if (hasBelow) return "아래에 인접글자";
                }
            }
            if (!hasIntersection) return "교차점 없음";
        } else {
            if (startRow < 0) return "시작행 음수";
            if (startRow + len > gridSize) return "범위초과(행)";
            if (startCol < 0 || startCol >= gridSize) return "열 범위 초과";
            if (startRow > 0 && grid[startRow - 1][startCol] != '\0') return "앞에 글자있음";
            if (startRow + len < gridSize && grid[startRow + len][startCol] != '\0') return "뒤에 글자있음";

            boolean hasIntersection = false;
            for (int i = 0; i < len; i++) {
                int row = startRow + i;
                char existing = grid[row][startCol];
                char newChar = word.charAt(i);

                if (existing != '\0') {
                    if (existing != newChar) return "교차점 글자불일치: " + existing + "!=" + newChar;
                    hasIntersection = true;
                } else {
                    boolean hasLeft = startCol > 0 && grid[row][startCol - 1] != '\0';
                    boolean hasRight = startCol < gridSize - 1 && grid[row][startCol + 1] != '\0';
                    if (hasLeft) return "좌측에 인접글자";
                    if (hasRight) return "우측에 인접글자";
                }
            }
            if (!hasIntersection) return "교차점 없음";
        }
        return "OK";
    }
}

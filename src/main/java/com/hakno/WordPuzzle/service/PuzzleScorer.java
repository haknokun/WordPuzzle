package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.dto.PuzzleCell;
import com.hakno.WordPuzzle.dto.PuzzleResponse;
import com.hakno.WordPuzzle.dto.PuzzleWord;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 퍼즐 품질 평가를 위한 스코어링 시스템
 * - intersectionCount: 단어 간 교차점 수
 * - density: 그리드 밀도 (사용된 셀 / 바운딩 박스 면적)
 * - balance: 4등분 분포 균형도 (0~1, 1이 완벽한 균형)
 * - connectivity: 연결성 (모든 단어가 연결되어 있는지)
 */
@Component
public class PuzzleScorer {

    // 스코어 가중치
    private static final double WEIGHT_INTERSECTION = 10.0;
    private static final double WEIGHT_DENSITY = 30.0;
    private static final double WEIGHT_BALANCE = 20.0;
    private static final double WEIGHT_CONNECTIVITY = 40.0;

    // 목표 밀도 (50% 이상이 좋은 퍼즐)
    public static final double TARGET_DENSITY = 0.5;

    /**
     * 퍼즐 전체 품질 점수 계산
     * @param puzzle 평가할 퍼즐
     * @return 품질 점수 (0~100)
     */
    public double calculateScore(PuzzleResponse puzzle) {
        PuzzleScore score = calculateDetailedScore(puzzle);
        return score.getTotalScore();
    }

    /**
     * 상세 점수 계산
     */
    public PuzzleScore calculateDetailedScore(PuzzleResponse puzzle) {
        List<List<PuzzleCell>> grid = puzzle.getGrid();
        int gridSize = puzzle.getGridSize();
        List<PuzzleWord> acrossWords = puzzle.getAcrossWords();
        List<PuzzleWord> downWords = puzzle.getDownWords();

        int intersectionCount = countIntersections(grid, gridSize);
        double density = calculateDensity(grid, gridSize);
        double balance = calculateBalance(grid, gridSize);
        double connectivity = calculateConnectivity(acrossWords, downWords);

        // 개별 점수 계산 (0~1 범위)
        double intersectionScore = normalizeIntersectionScore(intersectionCount, puzzle.getTotalWords());
        double densityScore = normalizeDensityScore(density);
        double balanceScore = balance;
        double connectivityScore = connectivity;

        // 가중 평균으로 총점 계산
        double totalScore = (intersectionScore * WEIGHT_INTERSECTION +
                           densityScore * WEIGHT_DENSITY +
                           balanceScore * WEIGHT_BALANCE +
                           connectivityScore * WEIGHT_CONNECTIVITY) /
                           (WEIGHT_INTERSECTION + WEIGHT_DENSITY + WEIGHT_BALANCE + WEIGHT_CONNECTIVITY) * 100;

        return PuzzleScore.builder()
                .intersectionCount(intersectionCount)
                .density(density)
                .balance(balance)
                .connectivity(connectivity)
                .intersectionScore(intersectionScore * 100)
                .densityScore(densityScore * 100)
                .balanceScore(balanceScore * 100)
                .connectivityScore(connectivityScore * 100)
                .totalScore(totalScore)
                .build();
    }

    /**
     * 교차점 수 계산
     * 하나의 셀이 가로와 세로 단어에 동시에 속하면 교차점
     */
    public int countIntersections(List<List<PuzzleCell>> grid, int gridSize) {
        int count = 0;
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                PuzzleCell cell = grid.get(row).get(col);
                if (!cell.isBlank() && cell.getAcrossNumber() != null && cell.getDownNumber() != null) {
                    // 이 셀이 가로와 세로 단어의 시작점인 경우
                    count++;
                } else if (!cell.isBlank()) {
                    // 시작점이 아니더라도 교차점인지 확인 (주변 셀 검사)
                    boolean hasHorizontalNeighbor =
                        (col > 0 && !grid.get(row).get(col - 1).isBlank()) ||
                        (col < gridSize - 1 && !grid.get(row).get(col + 1).isBlank());
                    boolean hasVerticalNeighbor =
                        (row > 0 && !grid.get(row - 1).get(col).isBlank()) ||
                        (row < gridSize - 1 && !grid.get(row + 1).get(col).isBlank());

                    if (hasHorizontalNeighbor && hasVerticalNeighbor) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * 그리드 밀도 계산
     * 밀도 = 채워진 셀 수 / 바운딩 박스 면적
     */
    public double calculateDensity(List<List<PuzzleCell>> grid, int gridSize) {
        int filledCells = 0;
        int minRow = gridSize, maxRow = 0, minCol = gridSize, maxCol = 0;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                if (!grid.get(row).get(col).isBlank()) {
                    filledCells++;
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        if (filledCells == 0) return 0.0;

        int boundingBoxArea = (maxRow - minRow + 1) * (maxCol - minCol + 1);
        return (double) filledCells / boundingBoxArea;
    }

    /**
     * char[][] 그리드용 밀도 계산 (내부 알고리즘용)
     */
    public double calculateDensity(char[][] grid, int gridSize) {
        int filledCells = 0;
        int minRow = gridSize, maxRow = 0, minCol = gridSize, maxCol = 0;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                if (grid[row][col] != '\0') {
                    filledCells++;
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        if (filledCells == 0) return 0.0;

        int boundingBoxArea = (maxRow - minRow + 1) * (maxCol - minCol + 1);
        return (double) filledCells / boundingBoxArea;
    }

    /**
     * 균형도 계산 (4등분 분포)
     * 그리드를 4등분하여 각 사분면의 채워진 셀 수 비교
     * 완벽하게 균형잡힌 경우 1.0, 한쪽에 치우친 경우 0에 가까움
     */
    public double calculateBalance(List<List<PuzzleCell>> grid, int gridSize) {
        int[] quadrantCounts = new int[4]; // [좌상, 우상, 좌하, 우하]
        int midRow = gridSize / 2;
        int midCol = gridSize / 2;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                if (!grid.get(row).get(col).isBlank()) {
                    int quadrant = (row < midRow ? 0 : 2) + (col < midCol ? 0 : 1);
                    quadrantCounts[quadrant]++;
                }
            }
        }

        int total = quadrantCounts[0] + quadrantCounts[1] + quadrantCounts[2] + quadrantCounts[3];
        if (total == 0) return 0.0;

        // 이상적인 분포: 각 사분면에 total/4개
        double ideal = total / 4.0;
        double variance = 0;
        for (int count : quadrantCounts) {
            variance += Math.pow(count - ideal, 2);
        }
        variance /= 4;

        // 표준편차를 정규화하여 균형 점수로 변환
        // 최대 표준편차는 모든 셀이 한 사분면에 있을 때: sqrt((total-ideal)^2 * 1 + ideal^2 * 3) / 4
        double maxVariance = Math.pow(total - ideal, 2) * 0.25 + Math.pow(ideal, 2) * 0.75;
        if (maxVariance == 0) return 1.0;

        return 1.0 - Math.sqrt(variance / maxVariance);
    }

    /**
     * char[][] 그리드용 균형도 계산
     */
    public double calculateBalance(char[][] grid, int gridSize) {
        int[] quadrantCounts = new int[4];
        int midRow = gridSize / 2;
        int midCol = gridSize / 2;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                if (grid[row][col] != '\0') {
                    int quadrant = (row < midRow ? 0 : 2) + (col < midCol ? 0 : 1);
                    quadrantCounts[quadrant]++;
                }
            }
        }

        int total = quadrantCounts[0] + quadrantCounts[1] + quadrantCounts[2] + quadrantCounts[3];
        if (total == 0) return 0.0;

        double ideal = total / 4.0;
        double variance = 0;
        for (int count : quadrantCounts) {
            variance += Math.pow(count - ideal, 2);
        }
        variance /= 4;

        double maxVariance = Math.pow(total - ideal, 2) * 0.25 + Math.pow(ideal, 2) * 0.75;
        if (maxVariance == 0) return 1.0;

        return 1.0 - Math.sqrt(variance / maxVariance);
    }

    /**
     * 연결성 계산
     * 모든 단어가 하나의 연결된 컴포넌트를 형성하는지 확인
     * 완전 연결: 1.0, 분리됨: 연결된 가장 큰 컴포넌트 비율
     */
    public double calculateConnectivity(List<PuzzleWord> acrossWords, List<PuzzleWord> downWords) {
        if (acrossWords.isEmpty() && downWords.isEmpty()) return 0.0;

        int totalWords = acrossWords.size() + downWords.size();
        if (totalWords <= 1) return 1.0;

        // Union-Find를 사용하여 연결된 단어 그룹 찾기
        int[] parent = new int[totalWords];
        for (int i = 0; i < totalWords; i++) {
            parent[i] = i;
        }

        // 교차하는 단어들을 union
        for (int i = 0; i < acrossWords.size(); i++) {
            PuzzleWord across = acrossWords.get(i);
            for (int j = 0; j < downWords.size(); j++) {
                PuzzleWord down = downWords.get(j);
                if (wordsIntersect(across, down)) {
                    union(parent, i, acrossWords.size() + j);
                }
            }
        }

        // 가장 큰 연결 컴포넌트 크기 계산
        int[] componentSize = new int[totalWords];
        for (int i = 0; i < totalWords; i++) {
            componentSize[find(parent, i)]++;
        }

        int maxComponent = 0;
        for (int size : componentSize) {
            maxComponent = Math.max(maxComponent, size);
        }

        return (double) maxComponent / totalWords;
    }

    /**
     * 두 단어가 교차하는지 확인
     */
    private boolean wordsIntersect(PuzzleWord across, PuzzleWord down) {
        // across는 가로 (row 고정, col 변화)
        // down은 세로 (col 고정, row 변화)
        int acrossRow = across.getStartRow();
        int downCol = down.getStartCol();

        // down의 시작 row ~ 끝 row 범위에 across의 row가 있는지
        boolean rowInRange = acrossRow >= down.getStartRow() &&
                            acrossRow < down.getStartRow() + down.getWord().length();

        // across의 시작 col ~ 끝 col 범위에 down의 col이 있는지
        boolean colInRange = downCol >= across.getStartCol() &&
                            downCol < across.getStartCol() + across.getWord().length();

        return rowInRange && colInRange;
    }

    private int find(int[] parent, int x) {
        if (parent[x] != x) {
            parent[x] = find(parent, parent[x]);
        }
        return parent[x];
    }

    private void union(int[] parent, int x, int y) {
        int px = find(parent, x);
        int py = find(parent, y);
        if (px != py) {
            parent[px] = py;
        }
    }

    /**
     * 교차점 점수 정규화
     * 이상적: 단어 수 - 1 (모든 단어가 최소 1개씩 교차)
     */
    private double normalizeIntersectionScore(int intersections, int totalWords) {
        if (totalWords <= 1) return 1.0;
        int idealIntersections = totalWords - 1;
        return Math.min(1.0, (double) intersections / idealIntersections);
    }

    /**
     * 밀도 점수 정규화
     * TARGET_DENSITY 이상이면 만점
     */
    private double normalizeDensityScore(double density) {
        return Math.min(1.0, density / TARGET_DENSITY);
    }

    /**
     * 퍼즐 점수 상세 정보
     */
    @Getter
    @Builder
    public static class PuzzleScore {
        private final int intersectionCount;
        private final double density;
        private final double balance;
        private final double connectivity;

        // 개별 점수 (0~100)
        private final double intersectionScore;
        private final double densityScore;
        private final double balanceScore;
        private final double connectivityScore;

        // 종합 점수 (0~100)
        private final double totalScore;

        /**
         * 점수 요약 문자열
         */
        public String getSummary() {
            return String.format(
                "총점: %.1f | 교차: %d개(%.1f점) | 밀도: %.1f%%(%.1f점) | 균형: %.1f점 | 연결: %.1f점",
                totalScore, intersectionCount, intersectionScore,
                density * 100, densityScore, balanceScore, connectivityScore
            );
        }
    }
}

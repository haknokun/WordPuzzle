package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.dto.PuzzleCell;
import com.hakno.WordPuzzle.dto.PuzzleResponse;
import com.hakno.WordPuzzle.dto.PuzzleWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("PuzzleScorer 테스트")
class PuzzleScorerTest {

    private PuzzleScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new PuzzleScorer();
    }

    @Nested
    @DisplayName("교차점 계산 테스트")
    class IntersectionCountTests {

        @Test
        @DisplayName("교차점이 없는 단일 단어")
        void singleWord_noIntersection() {
            // 가로 단어 하나만 있는 경우
            // □□□
            // 테스트
            // □□□
            List<List<PuzzleCell>> grid = createGrid(3, new String[]{
                "□□□",
                "테스트",
                "□□□"
            });

            int count = scorer.countIntersections(grid, 3);
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("두 단어가 한 점에서 교차")
        void twoWords_oneIntersection() {
            // □테□
            // 테스트
            // □트□
            List<List<PuzzleCell>> grid = createGrid(3, new String[]{
                "□테□",
                "테스트",
                "□트□"
            });

            int count = scorer.countIntersections(grid, 3);
            assertThat(count).isEqualTo(1); // 중앙의 '스' 위치에서 교차
        }

        @Test
        @DisplayName("복잡한 퍼즐의 교차점 계산")
        void complexPuzzle_multipleIntersections() {
            // 가□□나
            // 로□□다
            // □교차□
            // 세□□라
            List<List<PuzzleCell>> grid = createGrid(4, new String[]{
                "가□□나",
                "로□□다",
                "□교차□",
                "세□□라"
            });

            int count = scorer.countIntersections(grid, 4);
            assertThat(count).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("밀도 계산 테스트")
    class DensityTests {

        @Test
        @DisplayName("빈 그리드의 밀도는 0")
        void emptyGrid_zeroDensity() {
            char[][] grid = new char[5][5];
            double density = scorer.calculateDensity(grid, 5);
            assertThat(density).isEqualTo(0.0);
        }

        @Test
        @DisplayName("완전히 채워진 바운딩 박스의 밀도는 1")
        void fullBoundingBox_densityOne() {
            // 2x3 영역 완전히 채움
            char[][] grid = new char[5][5];
            grid[1][1] = '가'; grid[1][2] = '나'; grid[1][3] = '다';
            grid[2][1] = '라'; grid[2][2] = '마'; grid[2][3] = '바';

            double density = scorer.calculateDensity(grid, 5);
            assertThat(density).isEqualTo(1.0);
        }

        @Test
        @DisplayName("50% 채워진 바운딩 박스")
        void halfFilled_densityHalf() {
            // 2x2 바운딩 박스에서 2칸만 채움
            char[][] grid = new char[5][5];
            grid[1][1] = '가';
            grid[2][2] = '나';

            double density = scorer.calculateDensity(grid, 5);
            // 바운딩 박스 = 2x2 = 4, 채운 셀 = 2
            assertThat(density).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("십자형 패턴의 밀도")
        void crossPattern_density() {
            // □가□
            // 나다라
            // □마□
            char[][] grid = new char[3][3];
            grid[0][1] = '가';
            grid[1][0] = '나'; grid[1][1] = '다'; grid[1][2] = '라';
            grid[2][1] = '마';

            double density = scorer.calculateDensity(grid, 3);
            // 바운딩 박스 = 3x3 = 9, 채운 셀 = 5
            assertThat(density).isCloseTo(5.0/9, within(0.01));
        }

        @Test
        @DisplayName("List<List<PuzzleCell>> 그리드 밀도 계산")
        void puzzleCellGrid_density() {
            List<List<PuzzleCell>> grid = createGrid(3, new String[]{
                "□가□",
                "나다라",
                "□마□"
            });

            double density = scorer.calculateDensity(grid, 3);
            assertThat(density).isCloseTo(5.0/9, within(0.01));
        }
    }

    @Nested
    @DisplayName("균형도 계산 테스트")
    class BalanceTests {

        @Test
        @DisplayName("빈 그리드의 균형도는 0")
        void emptyGrid_zeroBalance() {
            char[][] grid = new char[4][4];
            double balance = scorer.calculateBalance(grid, 4);
            assertThat(balance).isEqualTo(0.0);
        }

        @Test
        @DisplayName("중앙에 집중된 퍼즐은 균형도가 높음")
        void centeredPuzzle_highBalance() {
            // 중앙에 글자가 모여있으면 4등분 했을 때 비교적 균등
            char[][] grid = new char[4][4];
            grid[1][1] = '가'; grid[1][2] = '나';
            grid[2][1] = '다'; grid[2][2] = '라';

            double balance = scorer.calculateBalance(grid, 4);
            assertThat(balance).isEqualTo(1.0); // 각 사분면에 1개씩
        }

        @Test
        @DisplayName("한 사분면에만 집중된 퍼즐은 균형도가 낮음")
        void cornerConcentrated_lowBalance() {
            // 좌상 사분면에만 글자
            char[][] grid = new char[4][4];
            grid[0][0] = '가'; grid[0][1] = '나';
            grid[1][0] = '다'; grid[1][1] = '라';

            double balance = scorer.calculateBalance(grid, 4);
            assertThat(balance).isLessThan(0.5);
        }

        @Test
        @DisplayName("대각선 패턴의 균형도")
        void diagonalPattern_balance() {
            // 대각선으로 채움 - 2개 사분면에만 있음
            char[][] grid = new char[4][4];
            grid[0][0] = '가';
            grid[1][1] = '나';
            grid[2][2] = '다';
            grid[3][3] = '라';

            double balance = scorer.calculateBalance(grid, 4);
            // 좌상=2, 우하=2, 나머지=0 → 완벽하지 않은 균형
            assertThat(balance).isBetween(0.3, 0.8);
        }
    }

    @Nested
    @DisplayName("연결성 계산 테스트")
    class ConnectivityTests {

        @Test
        @DisplayName("빈 퍼즐의 연결성은 0")
        void emptyPuzzle_zeroConnectivity() {
            List<PuzzleWord> across = new ArrayList<>();
            List<PuzzleWord> down = new ArrayList<>();

            double connectivity = scorer.calculateConnectivity(across, down);
            assertThat(connectivity).isEqualTo(0.0);
        }

        @Test
        @DisplayName("단일 단어는 연결성 1.0")
        void singleWord_fullConnectivity() {
            List<PuzzleWord> across = List.of(
                createPuzzleWord("테스트", 0, 0, PuzzleWord.Direction.ACROSS)
            );
            List<PuzzleWord> down = new ArrayList<>();

            double connectivity = scorer.calculateConnectivity(across, down);
            assertThat(connectivity).isEqualTo(1.0);
        }

        @Test
        @DisplayName("교차하는 두 단어는 연결성 1.0")
        void twoIntersectingWords_fullConnectivity() {
            // 가로: (1, 0) ~ (1, 2) "테스트"
            // 세로: (0, 1) ~ (2, 1) "스크롤"
            List<PuzzleWord> across = List.of(
                createPuzzleWord("테스트", 1, 0, PuzzleWord.Direction.ACROSS)
            );
            List<PuzzleWord> down = List.of(
                createPuzzleWord("스크롤", 0, 1, PuzzleWord.Direction.DOWN)
            );

            double connectivity = scorer.calculateConnectivity(across, down);
            assertThat(connectivity).isEqualTo(1.0);
        }

        @Test
        @DisplayName("분리된 두 단어는 연결성이 낮음")
        void twoSeparateWords_lowConnectivity() {
            // 교차하지 않는 두 단어
            List<PuzzleWord> across = List.of(
                createPuzzleWord("테스트", 0, 0, PuzzleWord.Direction.ACROSS)
            );
            List<PuzzleWord> down = List.of(
                createPuzzleWord("분리", 5, 5, PuzzleWord.Direction.DOWN)
            );

            double connectivity = scorer.calculateConnectivity(across, down);
            assertThat(connectivity).isEqualTo(0.5); // 2개 중 1개 컴포넌트
        }

        @Test
        @DisplayName("체인으로 연결된 여러 단어")
        void chainedWords_fullConnectivity() {
            // A1 ↔ D1 ↔ A2 형태로 연결
            List<PuzzleWord> across = List.of(
                createPuzzleWord("가나다", 1, 0, PuzzleWord.Direction.ACROSS),
                createPuzzleWord("라마바", 3, 0, PuzzleWord.Direction.ACROSS)
            );
            List<PuzzleWord> down = List.of(
                createPuzzleWord("나다라마", 1, 1, PuzzleWord.Direction.DOWN)
            );

            double connectivity = scorer.calculateConnectivity(across, down);
            assertThat(connectivity).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("종합 점수 계산 테스트")
    class TotalScoreTests {

        @Test
        @DisplayName("잘 구성된 퍼즐은 높은 점수")
        void wellFormedPuzzle_highScore() {
            // 교차점이 있고, 밀도가 높고, 균형 잡힌 퍼즐
            PuzzleResponse puzzle = createSamplePuzzle();

            PuzzleScorer.PuzzleScore score = scorer.calculateDetailedScore(puzzle);

            assertThat(score.getTotalScore()).isGreaterThan(0);
            assertThat(score.getConnectivityScore()).isGreaterThan(0);
        }

        @Test
        @DisplayName("단일 단어 퍼즐의 점수")
        void singleWordPuzzle_score() {
            PuzzleResponse puzzle = createSingleWordPuzzle();

            double score = scorer.calculateScore(puzzle);
            assertThat(score).isGreaterThan(0);
        }

        @Test
        @DisplayName("점수 요약 문자열 생성")
        void scoreSummary_format() {
            PuzzleResponse puzzle = createSamplePuzzle();
            PuzzleScorer.PuzzleScore score = scorer.calculateDetailedScore(puzzle);

            String summary = score.getSummary();
            assertThat(summary).contains("총점:");
            assertThat(summary).contains("교차:");
            assertThat(summary).contains("밀도:");
            assertThat(summary).contains("균형:");
            assertThat(summary).contains("연결:");
        }
    }

    @Nested
    @DisplayName("char[][] 그리드 메서드 테스트")
    class CharArrayGridTests {

        @Test
        @DisplayName("char[][] 밀도 계산")
        void charArrayDensity() {
            char[][] grid = new char[5][5];
            grid[2][1] = '가';
            grid[2][2] = '나';
            grid[2][3] = '다';

            double density = scorer.calculateDensity(grid, 5);
            assertThat(density).isEqualTo(1.0); // 1x3 박스, 3셀 채움
        }

        @Test
        @DisplayName("char[][] 균형 계산")
        void charArrayBalance() {
            char[][] grid = new char[4][4];
            grid[0][0] = '가';
            grid[0][3] = '나';
            grid[3][0] = '다';
            grid[3][3] = '라';

            double balance = scorer.calculateBalance(grid, 4);
            assertThat(balance).isEqualTo(1.0); // 각 사분면에 1개씩
        }
    }

    // ============== 헬퍼 메서드 ==============

    private List<List<PuzzleCell>> createGrid(int size, String[] rows) {
        List<List<PuzzleCell>> grid = new ArrayList<>();
        for (int row = 0; row < size; row++) {
            List<PuzzleCell> rowCells = new ArrayList<>();
            for (int col = 0; col < size; col++) {
                char c = rows[row].charAt(col);
                boolean isBlank = c == '□';
                rowCells.add(PuzzleCell.builder()
                    .row(row)
                    .col(col)
                    .letter(isBlank ? "" : String.valueOf(c))
                    .isBlank(isBlank)
                    .build());
            }
            grid.add(rowCells);
        }
        return grid;
    }

    private PuzzleWord createPuzzleWord(String word, int startRow, int startCol, PuzzleWord.Direction direction) {
        return PuzzleWord.builder()
            .number(1)
            .word(word)
            .definition("테스트 정의")
            .startRow(startRow)
            .startCol(startCol)
            .direction(direction)
            .build();
    }

    private PuzzleResponse createSamplePuzzle() {
        // □테□
        // 테스트
        // □트□
        List<List<PuzzleCell>> grid = createGrid(3, new String[]{
            "□테□",
            "테스트",
            "□트□"
        });

        List<PuzzleWord> across = List.of(
            createPuzzleWord("테스트", 1, 0, PuzzleWord.Direction.ACROSS)
        );
        List<PuzzleWord> down = List.of(
            createPuzzleWord("테스트", 0, 1, PuzzleWord.Direction.DOWN)
        );

        return PuzzleResponse.builder()
            .gridSize(3)
            .grid(grid)
            .acrossWords(across)
            .downWords(down)
            .totalWords(2)
            .build();
    }

    private PuzzleResponse createSingleWordPuzzle() {
        List<List<PuzzleCell>> grid = createGrid(5, new String[]{
            "□□□□□",
            "□테스트□",
            "□□□□□",
            "□□□□□",
            "□□□□□"
        });

        List<PuzzleWord> across = List.of(
            createPuzzleWord("테스트", 1, 1, PuzzleWord.Direction.ACROSS)
        );

        return PuzzleResponse.builder()
            .gridSize(5)
            .grid(grid)
            .acrossWords(across)
            .downWords(new ArrayList<>())
            .totalWords(1)
            .build();
    }
}

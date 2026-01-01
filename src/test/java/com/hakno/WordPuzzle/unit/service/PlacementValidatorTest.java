package com.hakno.WordPuzzle.unit.service;

import com.hakno.WordPuzzle.dto.PuzzleWord;
import com.hakno.WordPuzzle.service.PlacementValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PlacementValidator 단위 테스트
 * Phase 2: 단어 배치 검증 로직 테스트
 */
class PlacementValidatorTest {

    private PlacementValidator validator;
    private static final int GRID_SIZE = 15;

    @BeforeEach
    void setUp() {
        validator = new PlacementValidator();
    }

    @Nested
    @DisplayName("canPlaceWord - 가로 배치 테스트")
    class AcrossPlacementTest {

        @Test
        @DisplayName("빈 그리드에 첫 단어 배치 - 교차점 없이는 불가")
        void shouldNotPlaceFirstWordWithoutIntersection() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            String word = "사과";

            // When
            boolean result = validator.canPlaceWord(grid, word, 7, 6, PuzzleWord.Direction.ACROSS, GRID_SIZE);

            // Then - 첫 단어는 교차점이 없어서 false (기존 로직 유지)
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("교차점이 있는 경우 배치 가능")
        void shouldPlaceWordWithIntersection() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            grid[7][7] = '과'; // 세로 단어의 일부

            String word = "사과"; // '과'에서 교차

            // When
            boolean result = validator.canPlaceWord(grid, word, 7, 6, PuzzleWord.Direction.ACROSS, GRID_SIZE);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("범위 초과 시 배치 불가")
        void shouldNotPlaceWordOutOfBounds() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            grid[7][14] = '가';
            String word = "가나다"; // 14 + 3 = 17 > 15

            // When
            boolean result = validator.canPlaceWord(grid, word, 7, 14, PuzzleWord.Direction.ACROSS, GRID_SIZE);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("시작 위치가 음수인 경우 배치 불가")
        void shouldNotPlaceWordWithNegativeStart() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            grid[7][0] = '다';
            String word = "가나다"; // 시작 col = -2

            // When
            boolean result = validator.canPlaceWord(grid, word, 7, -2, PuzzleWord.Direction.ACROSS, GRID_SIZE);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("단어 앞에 글자가 있으면 배치 불가")
        void shouldNotPlaceWordWithCharBefore() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            grid[7][5] = '전'; // 단어 시작 전에 글자
            grid[7][7] = '과';
            String word = "사과";

            // When
            boolean result = validator.canPlaceWord(grid, word, 7, 6, PuzzleWord.Direction.ACROSS, GRID_SIZE);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("단어 뒤에 글자가 있으면 배치 불가")
        void shouldNotPlaceWordWithCharAfter() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            grid[7][7] = '과';
            grid[7][8] = '후'; // 단어 끝 뒤에 글자
            String word = "사과";

            // When
            boolean result = validator.canPlaceWord(grid, word, 7, 6, PuzzleWord.Direction.ACROSS, GRID_SIZE);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("교차점 글자 불일치 시 배치 불가")
        void shouldNotPlaceWordWithMismatchedIntersection() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            grid[7][7] = '배'; // '과'가 아닌 다른 글자
            String word = "사과";

            // When
            boolean result = validator.canPlaceWord(grid, word, 7, 6, PuzzleWord.Direction.ACROSS, GRID_SIZE);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("인접 셀에 글자가 있으면 배치 불가 (위쪽)")
        void shouldNotPlaceWordWithAdjacentCharAbove() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            grid[7][7] = '과'; // 교차점
            grid[6][6] = '상'; // 교차점이 아닌 셀의 위쪽에 글자
            String word = "사과";

            // When
            boolean result = validator.canPlaceWord(grid, word, 7, 6, PuzzleWord.Direction.ACROSS, GRID_SIZE);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("canPlaceWord - 세로 배치 테스트")
    class DownPlacementTest {

        @Test
        @DisplayName("교차점이 있는 경우 세로 배치 가능")
        void shouldPlaceWordDownWithIntersection() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            grid[7][7] = '나'; // 가로 단어의 일부
            String word = "나무"; // '나'에서 교차

            // When
            boolean result = validator.canPlaceWord(grid, word, 7, 7, PuzzleWord.Direction.DOWN, GRID_SIZE);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("세로 범위 초과 시 배치 불가")
        void shouldNotPlaceWordDownOutOfBounds() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            grid[14][7] = '가';
            String word = "가나다";

            // When
            boolean result = validator.canPlaceWord(grid, word, 14, 7, PuzzleWord.Direction.DOWN, GRID_SIZE);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("좌우 인접 글자 시 배치 불가")
        void shouldNotPlaceWordDownWithAdjacentCharLeft() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            grid[7][7] = '나'; // 교차점
            grid[8][6] = '좌'; // 교차점 아닌 셀의 좌측
            String word = "나무";

            // When
            boolean result = validator.canPlaceWord(grid, word, 7, 7, PuzzleWord.Direction.DOWN, GRID_SIZE);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("whyCannotPlace - 실패 원인 진단")
    class WhyCannotPlaceTest {

        @Test
        @DisplayName("범위 초과 원인 반환")
        void shouldReturnOutOfBoundsReason() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            String word = "가나다";

            // When
            String reason = validator.whyCannotPlace(grid, word, 7, 14, PuzzleWord.Direction.ACROSS, GRID_SIZE);

            // Then
            assertThat(reason).contains("범위");
        }

        @Test
        @DisplayName("교차점 없음 원인 반환")
        void shouldReturnNoIntersectionReason() {
            // Given
            char[][] grid = new char[GRID_SIZE][GRID_SIZE];
            String word = "사과";

            // When
            String reason = validator.whyCannotPlace(grid, word, 7, 6, PuzzleWord.Direction.ACROSS, GRID_SIZE);

            // Then
            assertThat(reason).contains("교차점");
        }
    }
}

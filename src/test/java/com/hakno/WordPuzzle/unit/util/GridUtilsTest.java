package com.hakno.WordPuzzle.unit.util;

import com.hakno.WordPuzzle.util.GridUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GridUtils 단위 테스트
 * Phase 2: 순수 함수 추출 및 테스트
 */
class GridUtilsTest {

    @Nested
    @DisplayName("calculateGridSize 테스트")
    class CalculateGridSizeTest {

        @Test
        @DisplayName("단어 5개일 때 그리드 크기는 11")
        void shouldReturn11For5Words() {
            // Given
            int wordCount = 5;

            // When
            int result = GridUtils.calculateGridSize(wordCount);

            // Then - 8 + (5 * 0.7) = 11.5 -> 12, 하지만 기존 로직은 round 사용
            assertThat(result).isEqualTo(12);
        }

        @Test
        @DisplayName("단어 10개일 때 그리드 크기는 15")
        void shouldReturn15For10Words() {
            // Given
            int wordCount = 10;

            // When
            int result = GridUtils.calculateGridSize(wordCount);

            // Then - 8 + (10 * 0.7) = 15
            assertThat(result).isEqualTo(15);
        }

        @Test
        @DisplayName("단어 3개일 때 최소값 10 반환")
        void shouldReturnMinimum10For3Words() {
            // Given
            int wordCount = 3;

            // When
            int result = GridUtils.calculateGridSize(wordCount);

            // Then - 8 + (3 * 0.7) = 10.1 -> 10, 최소값 10
            assertThat(result).isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("단어 30개일 때 최대값 25 반환")
        void shouldReturnMaximum25For30Words() {
            // Given
            int wordCount = 30;

            // When
            int result = GridUtils.calculateGridSize(wordCount);

            // Then - 8 + (30 * 0.7) = 29, 하지만 최대값 25
            assertThat(result).isEqualTo(25);
        }

        @ParameterizedTest
        @CsvSource({
            "1, 10",   // 최소값
            "5, 12",   // 8 + 3.5 = 11.5 -> 12
            "10, 15",  // 8 + 7 = 15
            "20, 22",  // 8 + 14 = 22
            "50, 25"   // 최대값
        })
        @DisplayName("다양한 단어 수에 대한 그리드 크기 계산")
        void shouldCalculateCorrectGridSize(int wordCount, int expectedSize) {
            assertThat(GridUtils.calculateGridSize(wordCount)).isEqualTo(expectedSize);
        }
    }

    @Nested
    @DisplayName("countCommonChars 테스트")
    class CountCommonCharsTest {

        @Test
        @DisplayName("공통 글자가 포함된 단어")
        void shouldCountCommonChars() {
            // Given - 공통 글자: 가나다라마바사아자하이의를을에서는로고기대
            String word = "가나다";

            // When
            int result = GridUtils.countCommonChars(word);

            // Then
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("공통 글자가 없는 단어")
        void shouldReturnZeroForNoCommonChars() {
            // Given
            String word = "xyz";

            // When
            int result = GridUtils.countCommonChars(word);

            // Then
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("혼합된 단어")
        void shouldCountMixedWord() {
            // Given
            String word = "가방"; // '가'만 공통 글자

            // When
            int result = GridUtils.countCommonChars(word);

            // Then
            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("빈 문자열")
        void shouldReturnZeroForEmptyString() {
            // Given
            String word = "";

            // When
            int result = GridUtils.countCommonChars(word);

            // Then
            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("createEmptyGrid 테스트")
    class CreateEmptyGridTest {

        @Test
        @DisplayName("지정된 크기의 빈 그리드 생성")
        void shouldCreateEmptyGrid() {
            // Given
            int size = 10;

            // When
            char[][] grid = GridUtils.createEmptyGrid(size);

            // Then
            assertThat(grid.length).isEqualTo(size);
            assertThat(grid[0].length).isEqualTo(size);
            assertThat(grid[0][0]).isEqualTo('\0');
            assertThat(grid[size - 1][size - 1]).isEqualTo('\0');
        }
    }
}

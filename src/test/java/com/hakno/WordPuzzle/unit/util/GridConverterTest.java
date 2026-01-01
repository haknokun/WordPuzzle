package com.hakno.WordPuzzle.unit.util;

import com.hakno.WordPuzzle.dto.PuzzleCell;
import com.hakno.WordPuzzle.dto.PuzzleWord;
import com.hakno.WordPuzzle.util.GridConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GridConverter 단위 테스트
 * Phase 2: 그리드 변환 로직 테스트
 */
class GridConverterTest {

    private GridConverter converter;

    @BeforeEach
    void setUp() {
        converter = new GridConverter();
    }

    @Nested
    @DisplayName("convertToCellGrid 테스트")
    class ConvertToCellGridTest {

        @Test
        @DisplayName("빈 그리드 변환")
        void shouldConvertEmptyGrid() {
            // Given
            int size = 5;
            char[][] grid = new char[size][size];
            List<PuzzleWord> acrossWords = Collections.emptyList();
            List<PuzzleWord> downWords = Collections.emptyList();

            // When
            List<List<PuzzleCell>> result = converter.convertToCellGrid(grid, acrossWords, downWords, size);

            // Then
            assertThat(result).hasSize(size);
            assertThat(result.get(0)).hasSize(size);
            assertThat(result.get(0).get(0).isBlank()).isTrue();
            assertThat(result.get(0).get(0).getLetter()).isNull();
        }

        @Test
        @DisplayName("글자가 있는 셀 변환")
        void shouldConvertCellWithLetter() {
            // Given
            int size = 5;
            char[][] grid = new char[size][size];
            grid[2][2] = '가';
            List<PuzzleWord> acrossWords = Collections.emptyList();
            List<PuzzleWord> downWords = Collections.emptyList();

            // When
            List<List<PuzzleCell>> result = converter.convertToCellGrid(grid, acrossWords, downWords, size);

            // Then
            PuzzleCell cell = result.get(2).get(2);
            assertThat(cell.isBlank()).isFalse();
            assertThat(cell.getLetter()).isEqualTo("가");
            assertThat(cell.getRow()).isEqualTo(2);
            assertThat(cell.getCol()).isEqualTo(2);
        }

        @Test
        @DisplayName("가로 단어 번호 할당")
        void shouldAssignAcrossNumber() {
            // Given
            int size = 5;
            char[][] grid = new char[size][size];
            grid[2][1] = '사';
            grid[2][2] = '과';

            PuzzleWord acrossWord = PuzzleWord.builder()
                    .number(1)
                    .word("사과")
                    .startRow(2)
                    .startCol(1)
                    .direction(PuzzleWord.Direction.ACROSS)
                    .build();

            List<PuzzleWord> acrossWords = Arrays.asList(acrossWord);
            List<PuzzleWord> downWords = Collections.emptyList();

            // When
            List<List<PuzzleCell>> result = converter.convertToCellGrid(grid, acrossWords, downWords, size);

            // Then
            PuzzleCell startCell = result.get(2).get(1);
            assertThat(startCell.getAcrossNumber()).isEqualTo(1);

            // 두 번째 글자는 시작점이 아니므로 번호 없음
            PuzzleCell secondCell = result.get(2).get(2);
            assertThat(secondCell.getAcrossNumber()).isNull();
        }

        @Test
        @DisplayName("세로 단어 번호 할당")
        void shouldAssignDownNumber() {
            // Given
            int size = 5;
            char[][] grid = new char[size][size];
            grid[1][2] = '나';
            grid[2][2] = '무';

            PuzzleWord downWord = PuzzleWord.builder()
                    .number(1)
                    .word("나무")
                    .startRow(1)
                    .startCol(2)
                    .direction(PuzzleWord.Direction.DOWN)
                    .build();

            List<PuzzleWord> acrossWords = Collections.emptyList();
            List<PuzzleWord> downWords = Arrays.asList(downWord);

            // When
            List<List<PuzzleCell>> result = converter.convertToCellGrid(grid, acrossWords, downWords, size);

            // Then
            PuzzleCell startCell = result.get(1).get(2);
            assertThat(startCell.getDownNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 셀에 가로/세로 번호 모두 할당")
        void shouldAssignBothNumbersToSameCell() {
            // Given
            int size = 5;
            char[][] grid = new char[size][size];
            grid[2][2] = '가';

            PuzzleWord acrossWord = PuzzleWord.builder()
                    .number(1)
                    .word("가나")
                    .startRow(2)
                    .startCol(2)
                    .direction(PuzzleWord.Direction.ACROSS)
                    .build();

            PuzzleWord downWord = PuzzleWord.builder()
                    .number(2)
                    .word("가다")
                    .startRow(2)
                    .startCol(2)
                    .direction(PuzzleWord.Direction.DOWN)
                    .build();

            List<PuzzleWord> acrossWords = Arrays.asList(acrossWord);
            List<PuzzleWord> downWords = Arrays.asList(downWord);

            // When
            List<List<PuzzleCell>> result = converter.convertToCellGrid(grid, acrossWords, downWords, size);

            // Then
            PuzzleCell cell = result.get(2).get(2);
            assertThat(cell.getAcrossNumber()).isEqualTo(1);
            assertThat(cell.getDownNumber()).isEqualTo(2);
        }
    }
}

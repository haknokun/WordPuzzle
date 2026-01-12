package com.hakno.WordPuzzle.util;

import com.hakno.WordPuzzle.dto.PuzzleWord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GridSnapshot 테스트")
class GridSnapshotTest {

    @Nested
    @DisplayName("스냅샷 생성 테스트")
    class CreationTests {

        @Test
        @DisplayName("빈 그리드 스냅샷 생성")
        void createSnapshot_emptyGrid() {
            // given
            char[][] grid = new char[5][5];
            List<PuzzleWord> placedWords = new ArrayList<>();
            Set<String> usedWords = new HashSet<>();

            // when
            GridSnapshot snapshot = new GridSnapshot(grid, placedWords, usedWords);

            // then
            assertThat(snapshot.getGridSize()).isEqualTo(5);
            assertThat(snapshot.getPlacedWordCount()).isEqualTo(0);
            assertThat(snapshot.getFilledCellCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("채워진 그리드 스냅샷 생성")
        void createSnapshot_filledGrid() {
            // given
            char[][] grid = new char[5][5];
            grid[2][1] = '가';
            grid[2][2] = '나';
            grid[2][3] = '다';

            List<PuzzleWord> placedWords = List.of(
                createPuzzleWord("가나다", 2, 1, PuzzleWord.Direction.ACROSS)
            );
            Set<String> usedWords = new HashSet<>(Set.of("가나다"));

            // when
            GridSnapshot snapshot = new GridSnapshot(grid, placedWords, usedWords);

            // then
            assertThat(snapshot.getPlacedWordCount()).isEqualTo(1);
            assertThat(snapshot.getFilledCellCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("원본 그리드 변경해도 스냅샷 영향 없음")
        void createSnapshot_independentCopy() {
            // given
            char[][] grid = new char[3][3];
            grid[1][1] = '가';
            List<PuzzleWord> placedWords = new ArrayList<>();
            placedWords.add(createPuzzleWord("가", 1, 1, PuzzleWord.Direction.ACROSS));
            Set<String> usedWords = new HashSet<>();
            usedWords.add("가");

            GridSnapshot snapshot = new GridSnapshot(grid, placedWords, usedWords);

            // when - 원본 변경
            grid[1][1] = '나';
            placedWords.clear();
            usedWords.clear();

            // then - 스냅샷은 변경되지 않음
            assertThat(snapshot.getGridState()[1][1]).isEqualTo('가');
            assertThat(snapshot.getPlacedWords()).hasSize(1);
            assertThat(snapshot.getUsedWords()).contains("가");
        }
    }

    @Nested
    @DisplayName("복원 테스트")
    class RestoreTests {

        @Test
        @DisplayName("그리드 상태 복원")
        void restoreTo_restoresGrid() {
            // given - 초기 상태 스냅샷
            char[][] originalGrid = new char[3][3];
            originalGrid[1][0] = '가';
            originalGrid[1][1] = '나';
            originalGrid[1][2] = '다';

            List<PuzzleWord> originalWords = new ArrayList<>();
            originalWords.add(createPuzzleWord("가나다", 1, 0, PuzzleWord.Direction.ACROSS));
            Set<String> originalUsed = new HashSet<>(Set.of("가나다"));

            GridSnapshot snapshot = new GridSnapshot(originalGrid, originalWords, originalUsed);

            // 그리드 변경
            char[][] modifiedGrid = new char[3][3];
            modifiedGrid[0][1] = '라';
            modifiedGrid[1][1] = '나';
            modifiedGrid[2][1] = '마';

            List<PuzzleWord> modifiedWords = new ArrayList<>();
            modifiedWords.add(createPuzzleWord("라나마", 0, 1, PuzzleWord.Direction.DOWN));
            Set<String> modifiedUsed = new HashSet<>(Set.of("라나마"));

            // when - 복원
            snapshot.restoreTo(modifiedGrid, modifiedWords, modifiedUsed);

            // then
            assertThat(modifiedGrid[1][0]).isEqualTo('가');
            assertThat(modifiedGrid[1][1]).isEqualTo('나');
            assertThat(modifiedGrid[1][2]).isEqualTo('다');
            assertThat(modifiedGrid[0][1]).isEqualTo('\0'); // 덮어씌워짐
            assertThat(modifiedWords).hasSize(1);
            assertThat(modifiedWords.get(0).getWord()).isEqualTo("가나다");
            assertThat(modifiedUsed).containsExactly("가나다");
        }

        @Test
        @DisplayName("빈 상태로 복원")
        void restoreTo_restoresEmptyState() {
            // given
            char[][] emptyGrid = new char[3][3];
            List<PuzzleWord> emptyWords = new ArrayList<>();
            Set<String> emptyUsed = new HashSet<>();

            GridSnapshot snapshot = new GridSnapshot(emptyGrid, emptyWords, emptyUsed);

            // 그리드에 데이터 추가
            char[][] filledGrid = new char[3][3];
            filledGrid[1][1] = '가';
            List<PuzzleWord> filledWords = new ArrayList<>();
            filledWords.add(createPuzzleWord("가", 1, 1, PuzzleWord.Direction.ACROSS));
            Set<String> filledUsed = new HashSet<>(Set.of("가"));

            // when
            snapshot.restoreTo(filledGrid, filledWords, filledUsed);

            // then
            assertThat(filledGrid[1][1]).isEqualTo('\0');
            assertThat(filledWords).isEmpty();
            assertThat(filledUsed).isEmpty();
        }
    }

    @Nested
    @DisplayName("유틸리티 메서드 테스트")
    class UtilityTests {

        @Test
        @DisplayName("채워진 셀 수 계산")
        void getFilledCellCount_countsCorrectly() {
            // given
            char[][] grid = new char[4][4];
            grid[1][1] = '가';
            grid[1][2] = '나';
            grid[2][1] = '다';

            GridSnapshot snapshot = new GridSnapshot(grid, new ArrayList<>(), new HashSet<>());

            // when & then
            assertThat(snapshot.getFilledCellCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("그리드 문자열 표현")
        void toGridString_formatsCorrectly() {
            // given
            char[][] grid = new char[2][2];
            grid[0][0] = '가';
            grid[0][1] = '나';

            GridSnapshot snapshot = new GridSnapshot(grid, new ArrayList<>(), new HashSet<>());

            // when
            String gridStr = snapshot.toGridString();

            // then
            assertThat(gridStr).contains("가나");
            assertThat(gridStr).contains("□□"); // 빈 셀
        }

        @Test
        @DisplayName("toString 형식")
        void toString_format() {
            // given
            char[][] grid = new char[3][3];
            grid[1][1] = '가';

            List<PuzzleWord> words = List.of(
                createPuzzleWord("가", 1, 1, PuzzleWord.Direction.ACROSS)
            );

            GridSnapshot snapshot = new GridSnapshot(grid, words, new HashSet<>());

            // when
            String str = snapshot.toString();

            // then
            assertThat(str).contains("GridSnapshot");
            assertThat(str).contains("words=1");
            assertThat(str).contains("cells=1");
        }
    }

    @Nested
    @DisplayName("경계 케이스 테스트")
    class EdgeCaseTests {

        @Test
        @DisplayName("단일 셀 그리드")
        void singleCellGrid() {
            // given
            char[][] grid = new char[1][1];
            grid[0][0] = '가';

            // when
            GridSnapshot snapshot = new GridSnapshot(grid, new ArrayList<>(), new HashSet<>());

            // then
            assertThat(snapshot.getGridSize()).isEqualTo(1);
            assertThat(snapshot.getFilledCellCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("큰 그리드")
        void largeGrid() {
            // given
            int size = 30;
            char[][] grid = new char[size][size];
            // 대각선으로 채움
            for (int i = 0; i < size; i++) {
                grid[i][i] = '가';
            }

            // when
            GridSnapshot snapshot = new GridSnapshot(grid, new ArrayList<>(), new HashSet<>());

            // then
            assertThat(snapshot.getGridSize()).isEqualTo(size);
            assertThat(snapshot.getFilledCellCount()).isEqualTo(size);
        }

        @Test
        @DisplayName("여러 단어가 있는 스냅샷")
        void multipleWords() {
            // given
            char[][] grid = new char[5][5];
            grid[2][1] = '가'; grid[2][2] = '나'; grid[2][3] = '다';
            grid[1][2] = '라'; grid[3][2] = '마';

            List<PuzzleWord> words = new ArrayList<>();
            words.add(createPuzzleWord("가나다", 2, 1, PuzzleWord.Direction.ACROSS));
            words.add(createPuzzleWord("라나마", 1, 2, PuzzleWord.Direction.DOWN));

            Set<String> used = new HashSet<>(Set.of("가나다", "라나마"));

            // when
            GridSnapshot snapshot = new GridSnapshot(grid, words, used);

            // then
            assertThat(snapshot.getPlacedWordCount()).isEqualTo(2);
            assertThat(snapshot.getFilledCellCount()).isEqualTo(5); // '나'는 공유
            assertThat(snapshot.getUsedWords()).containsExactlyInAnyOrder("가나다", "라나마");
        }
    }

    // ============== 헬퍼 메서드 ==============

    private PuzzleWord createPuzzleWord(String word, int startRow, int startCol, PuzzleWord.Direction direction) {
        return PuzzleWord.builder()
            .number(1)
            .word(word)
            .definition("테스트")
            .startRow(startRow)
            .startCol(startCol)
            .direction(direction)
            .build();
    }
}

package com.hakno.WordPuzzle.unit.service;

import com.hakno.WordPuzzle.dto.PuzzleResponse;
import com.hakno.WordPuzzle.dto.PuzzleWord;
import com.hakno.WordPuzzle.entity.Definition;
import com.hakno.WordPuzzle.entity.Word;
import com.hakno.WordPuzzle.repository.WordRepository;
import com.hakno.WordPuzzle.service.PlacementValidator;
import com.hakno.WordPuzzle.service.PuzzleGeneratorService;
import com.hakno.WordPuzzle.util.GridConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * PuzzleGeneratorService 단위 테스트
 * Task 1: Backend PuzzleGeneratorService 테스트 추가
 */
@ExtendWith(MockitoExtension.class)
class PuzzleGeneratorServiceTest {

    @Mock
    private WordRepository wordRepository;

    private PlacementValidator placementValidator;
    private GridConverter gridConverter;
    private PuzzleGeneratorService puzzleGeneratorService;

    @BeforeEach
    void setUp() {
        placementValidator = new PlacementValidator();
        gridConverter = new GridConverter();
        puzzleGeneratorService = new PuzzleGeneratorService(
            wordRepository, placementValidator, gridConverter
        );
    }

    /**
     * 테스트용 Word 엔티티 생성
     */
    private Word createWordWithDefinition(String wordStr, String definitionStr) {
        Word word = Word.builder()
            .word(wordStr)
            .partOfSpeech("명사")
            .vocabularyLevel("초급")
            .build();

        Definition definition = Definition.builder()
            .senseOrder(1)
            .definition(definitionStr)
            .build();
        word.addDefinition(definition);

        return word;
    }

    @Nested
    @DisplayName("generatePuzzle - 퍼즐 생성 통합 테스트")
    class GeneratePuzzleTest {

        @Test
        @DisplayName("정상적인 퍼즐 생성 - 단어 목록이 있을 때")
        void shouldGeneratePuzzleWithWords() {
            // Given
            Word word1 = createWordWithDefinition("사과", "과일의 한 종류");
            Word word2 = createWordWithDefinition("과일", "먹을 수 있는 식물의 열매");
            Word word3 = createWordWithDefinition("나무", "식물의 한 종류");
            Word word4 = createWordWithDefinition("무지개", "비 온 뒤 하늘에 나타나는 현상");

            // 첫 번째 단어 검색 - findRandomWordsWithDefinitionsByLevel
            when(wordRepository.findRandomWordsWithDefinitionsByLevel(
                anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Arrays.asList(word1, word2, word3));

            // 교차 단어 검색 - findByContainingCharacterWithDefinitionsByLevel
            when(wordRepository.findByContainingCharacterWithDefinitionsByLevel(
                anyString(), anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Arrays.asList(word2, word3, word4));

            // When
            PuzzleResponse response = puzzleGeneratorService.generatePuzzle(10, 3);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getGridSize()).isEqualTo(10);
            assertThat(response.getTotalWords()).isGreaterThanOrEqualTo(1);
            assertThat(response.getGrid()).isNotNull();
            assertThat(response.getGrid()).hasSize(10);
        }

        @Test
        @DisplayName("단어 데이터가 없을 때 예외 발생")
        void shouldThrowExceptionWhenNoWords() {
            // Given
            when(wordRepository.findRandomWordsWithDefinitionsByLevel(
                anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> puzzleGeneratorService.generatePuzzle(10, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("단어 데이터가 없습니다");
        }

        @Test
        @DisplayName("gridSize가 null일 때 자동 계산")
        void shouldAutoCalculateGridSizeWhenNull() {
            // Given
            Word word1 = createWordWithDefinition("사과", "과일");
            when(wordRepository.findRandomWordsWithDefinitionsByLevel(
                anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Arrays.asList(word1));
            when(wordRepository.findByContainingCharacterWithDefinitionsByLevel(
                anyString(), anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

            // When
            PuzzleResponse response = puzzleGeneratorService.generatePuzzle(null, 5);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getGridSize()).isGreaterThan(0);
        }

        @Test
        @DisplayName("난이도 레벨로 퍼즐 생성")
        void shouldGeneratePuzzleWithLevel() {
            // Given
            Word word1 = createWordWithDefinition("사과", "과일");
            String level = "초급";

            when(wordRepository.findRandomWordsWithDefinitionsByLevel(
                anyInt(), anyInt(), eq(level), any(PageRequest.class)))
                .thenReturn(Arrays.asList(word1));
            when(wordRepository.findByContainingCharacterWithDefinitionsByLevel(
                anyString(), anyInt(), anyInt(), eq(level), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

            // When
            PuzzleResponse response = puzzleGeneratorService.generatePuzzle(10, 3, level);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalWords()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("목표 단어 수의 70% 미만이면 재시도")
        void shouldRetryWhenBelowTargetThreshold() {
            // Given
            Word word1 = createWordWithDefinition("가나다라", "테스트 단어");

            when(wordRepository.findRandomWordsWithDefinitionsByLevel(
                anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Arrays.asList(word1));
            when(wordRepository.findByContainingCharacterWithDefinitionsByLevel(
                anyString(), anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

            // When - 목표 10개인데 교차 단어가 없어서 1개만 배치됨
            PuzzleResponse response = puzzleGeneratorService.generatePuzzle(15, 10);

            // Then - 최소 1개는 있어야 함 (첫 단어)
            assertThat(response.getTotalWords()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("tryGeneratePuzzle - 단일 시도 테스트")
    class TryGeneratePuzzleTest {

        @Test
        @DisplayName("첫 번째 단어가 그리드 중앙에 배치됨")
        void shouldPlaceFirstWordAtCenter() {
            // Given
            Word word1 = createWordWithDefinition("가나다", "테스트");
            int gridSize = 11;

            when(wordRepository.findRandomWordsWithDefinitionsByLevel(
                anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Arrays.asList(word1));
            when(wordRepository.findByContainingCharacterWithDefinitionsByLevel(
                anyString(), anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

            // When
            PuzzleResponse response = puzzleGeneratorService.generatePuzzle(gridSize, 3);

            // Then
            assertThat(response.getAcrossWords()).isNotEmpty();
            PuzzleWord firstWord = response.getAcrossWords().get(0);
            assertThat(firstWord.getStartRow()).isEqualTo(gridSize / 2); // 중앙 행
            assertThat(firstWord.getDirection()).isEqualTo(PuzzleWord.Direction.ACROSS);
        }

        @Test
        @DisplayName("가로/세로 단어가 번호 순으로 정렬됨")
        void shouldSortAndNumberWords() {
            // Given
            Word word1 = createWordWithDefinition("가나다", "첫 번째");
            Word word2 = createWordWithDefinition("나비", "두 번째");

            when(wordRepository.findRandomWordsWithDefinitionsByLevel(
                anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Arrays.asList(word1));
            when(wordRepository.findByContainingCharacterWithDefinitionsByLevel(
                anyString(), anyInt(), anyInt(), isNull(), any(PageRequest.class)))
                .thenReturn(Arrays.asList(word2));

            // When
            PuzzleResponse response = puzzleGeneratorService.generatePuzzle(15, 5);

            // Then
            if (!response.getAcrossWords().isEmpty()) {
                PuzzleWord first = response.getAcrossWords().get(0);
                assertThat(first.getNumber()).isEqualTo(1);
            }
            if (response.getAcrossWords().size() > 1) {
                PuzzleWord second = response.getAcrossWords().get(1);
                assertThat(second.getNumber()).isEqualTo(2);
            }
        }
    }

    @Nested
    @DisplayName("findIntersectionCandidates - 교차점 후보 탐색 (리플렉션)")
    class FindIntersectionCandidatesTest {

        @Test
        @DisplayName("빈 그리드에서는 교차점 없음")
        void shouldReturnEmptyForEmptyGrid() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            int gridSize = 10;

            // When - 리플렉션으로 private 메서드 호출
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "findIntersectionCandidates", char[][].class, int.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<?> candidates = (List<?>) method.invoke(puzzleGeneratorService, grid, gridSize);

            // Then
            assertThat(candidates).isEmpty();
        }

        @Test
        @DisplayName("가로 단어가 있을 때 세로 교차점 후보 생성")
        void shouldFindDownCandidatesForHorizontalWord() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            // 가로 단어 배치: "가나다" at row 5
            grid[5][3] = '가';
            grid[5][4] = '나';
            grid[5][5] = '다';
            int gridSize = 10;

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "findIntersectionCandidates", char[][].class, int.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<?> candidates = (List<?>) method.invoke(puzzleGeneratorService, grid, gridSize);

            // Then - 세로 방향 후보가 있어야 함
            assertThat(candidates).isNotEmpty();
        }

        @Test
        @DisplayName("세로 단어가 있을 때 가로 교차점 후보 생성")
        void shouldFindAcrossCandidatesForVerticalWord() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            // 세로 단어 배치: "가나다" at col 5
            grid[3][5] = '가';
            grid[4][5] = '나';
            grid[5][5] = '다';
            int gridSize = 10;

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "findIntersectionCandidates", char[][].class, int.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<?> candidates = (List<?>) method.invoke(puzzleGeneratorService, grid, gridSize);

            // Then - 가로 방향 후보가 있어야 함
            assertThat(candidates).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("placeWord - 단어 배치 (리플렉션)")
    class PlaceWordTest {

        @Test
        @DisplayName("가로 방향으로 단어 배치")
        void shouldPlaceWordAcross() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            String word = "테스트";
            int startRow = 5;
            int startCol = 3;

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "placeWord", char[][].class, String.class, int.class, int.class, PuzzleWord.Direction.class);
            method.setAccessible(true);
            method.invoke(puzzleGeneratorService, grid, word, startRow, startCol, PuzzleWord.Direction.ACROSS);

            // Then
            assertThat(grid[5][3]).isEqualTo('테');
            assertThat(grid[5][4]).isEqualTo('스');
            assertThat(grid[5][5]).isEqualTo('트');
        }

        @Test
        @DisplayName("세로 방향으로 단어 배치")
        void shouldPlaceWordDown() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            String word = "세로";
            int startRow = 2;
            int startCol = 5;

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "placeWord", char[][].class, String.class, int.class, int.class, PuzzleWord.Direction.class);
            method.setAccessible(true);
            method.invoke(puzzleGeneratorService, grid, word, startRow, startCol, PuzzleWord.Direction.DOWN);

            // Then
            assertThat(grid[2][5]).isEqualTo('세');
            assertThat(grid[3][5]).isEqualTo('로');
        }

        @Test
        @DisplayName("기존 글자와 교차하여 배치")
        void shouldPlaceWordWithIntersection() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            // 기존 가로 단어 "가나다" 배치
            grid[5][3] = '가';
            grid[5][4] = '나';
            grid[5][5] = '다';

            // 세로 단어 "나무" 배치 ('나'에서 교차)
            String word = "나무";
            int startRow = 5;
            int startCol = 4;

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "placeWord", char[][].class, String.class, int.class, int.class, PuzzleWord.Direction.class);
            method.setAccessible(true);
            method.invoke(puzzleGeneratorService, grid, word, startRow, startCol, PuzzleWord.Direction.DOWN);

            // Then - 교차점은 동일하고, 새 글자가 추가됨
            assertThat(grid[5][4]).isEqualTo('나'); // 교차점
            assertThat(grid[6][4]).isEqualTo('무'); // 새 글자
        }
    }

    @Nested
    @DisplayName("createPuzzleWord - PuzzleWord 생성 (리플렉션)")
    class CreatePuzzleWordTest {

        @Test
        @DisplayName("Word 엔티티로 PuzzleWord 생성")
        void shouldCreatePuzzleWordFromEntity() throws Exception {
            // Given
            Word word = createWordWithDefinition("사과", "맛있는 과일");

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "createPuzzleWord", Word.class, int.class, int.class, int.class, PuzzleWord.Direction.class);
            method.setAccessible(true);
            PuzzleWord puzzleWord = (PuzzleWord) method.invoke(
                puzzleGeneratorService, word, 1, 5, 3, PuzzleWord.Direction.ACROSS);

            // Then
            assertThat(puzzleWord.getWord()).isEqualTo("사과");
            assertThat(puzzleWord.getDefinition()).isEqualTo("맛있는 과일");
            assertThat(puzzleWord.getNumber()).isEqualTo(1);
            assertThat(puzzleWord.getStartRow()).isEqualTo(5);
            assertThat(puzzleWord.getStartCol()).isEqualTo(3);
            assertThat(puzzleWord.getDirection()).isEqualTo(PuzzleWord.Direction.ACROSS);
        }

        @Test
        @DisplayName("정의가 없는 Word에서 PuzzleWord 생성")
        void shouldCreatePuzzleWordWithEmptyDefinition() throws Exception {
            // Given
            Word word = Word.builder()
                .word("테스트")
                .partOfSpeech("명사")
                .build();
            // 정의 없음

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "createPuzzleWord", Word.class, int.class, int.class, int.class, PuzzleWord.Direction.class);
            method.setAccessible(true);
            PuzzleWord puzzleWord = (PuzzleWord) method.invoke(
                puzzleGeneratorService, word, 1, 0, 0, PuzzleWord.Direction.DOWN);

            // Then
            assertThat(puzzleWord.getWord()).isEqualTo("테스트");
            assertThat(puzzleWord.getDefinition()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isPartOfHorizontalWord / isPartOfVerticalWord - 단어 방향 판단 (리플렉션)")
    class WordDirectionCheckTest {

        @Test
        @DisplayName("가로 단어의 일부인지 확인")
        void shouldCheckIfPartOfHorizontalWord() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            grid[5][3] = '가';
            grid[5][4] = '나';
            grid[5][5] = '다';

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "isPartOfHorizontalWord", char[][].class, int.class, int.class);
            method.setAccessible(true);

            boolean result1 = (boolean) method.invoke(puzzleGeneratorService, grid, 5, 4); // 중간
            boolean result2 = (boolean) method.invoke(puzzleGeneratorService, grid, 5, 3); // 시작
            boolean result3 = (boolean) method.invoke(puzzleGeneratorService, grid, 5, 5); // 끝
            boolean result4 = (boolean) method.invoke(puzzleGeneratorService, grid, 3, 3); // 빈 곳

            // Then
            assertThat(result1).isTrue();  // 양쪽에 글자
            assertThat(result2).isTrue();  // 오른쪽에 글자
            assertThat(result3).isTrue();  // 왼쪽에 글자
            assertThat(result4).isFalse(); // 주변에 글자 없음
        }

        @Test
        @DisplayName("세로 단어의 일부인지 확인")
        void shouldCheckIfPartOfVerticalWord() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            grid[3][5] = '가';
            grid[4][5] = '나';
            grid[5][5] = '다';

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "isPartOfVerticalWord", char[][].class, int.class, int.class);
            method.setAccessible(true);

            boolean result1 = (boolean) method.invoke(puzzleGeneratorService, grid, 4, 5); // 중간
            boolean result2 = (boolean) method.invoke(puzzleGeneratorService, grid, 3, 5); // 시작
            boolean result3 = (boolean) method.invoke(puzzleGeneratorService, grid, 5, 5); // 끝
            boolean result4 = (boolean) method.invoke(puzzleGeneratorService, grid, 3, 3); // 빈 곳

            // Then
            assertThat(result1).isTrue();  // 위아래에 글자
            assertThat(result2).isTrue();  // 아래에 글자
            assertThat(result3).isTrue();  // 위에 글자
            assertThat(result4).isFalse(); // 주변에 글자 없음
        }
    }

    @Nested
    @DisplayName("canExtendHorizontally / canExtendVertically - 확장 가능 여부 (리플렉션)")
    class ExtensionCheckTest {

        @Test
        @DisplayName("가로 확장 가능 여부 확인")
        void shouldCheckCanExtendHorizontally() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            grid[5][5] = '가';
            int gridSize = 10;

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "canExtendHorizontally", char[][].class, int.class, int.class, int.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(puzzleGeneratorService, grid, 5, 5, gridSize);

            // Then - 좌우가 비어있으므로 확장 가능
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("세로 확장 가능 여부 확인")
        void shouldCheckCanExtendVertically() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            grid[5][5] = '가';
            int gridSize = 10;

            // When
            Method method = PuzzleGeneratorService.class.getDeclaredMethod(
                "canExtendVertically", char[][].class, int.class, int.class, int.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(puzzleGeneratorService, grid, 5, 5, gridSize);

            // Then - 상하가 비어있으므로 확장 가능
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("경계에서 확장 가능 여부")
        void shouldCheckExtensionAtBoundary() throws Exception {
            // Given
            char[][] grid = new char[10][10];
            grid[0][0] = '가'; // 좌상단 코너
            int gridSize = 10;

            Method hMethod = PuzzleGeneratorService.class.getDeclaredMethod(
                "canExtendHorizontally", char[][].class, int.class, int.class, int.class);
            hMethod.setAccessible(true);

            Method vMethod = PuzzleGeneratorService.class.getDeclaredMethod(
                "canExtendVertically", char[][].class, int.class, int.class, int.class);
            vMethod.setAccessible(true);

            // When
            boolean canExtendH = (boolean) hMethod.invoke(puzzleGeneratorService, grid, 0, 0, gridSize);
            boolean canExtendV = (boolean) vMethod.invoke(puzzleGeneratorService, grid, 0, 0, gridSize);

            // Then - 오른쪽/아래만 확장 가능
            assertThat(canExtendH).isTrue();
            assertThat(canExtendV).isTrue();
        }
    }
}

package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.dto.PuzzleResponse;
import com.hakno.WordPuzzle.entity.StdSense;
import com.hakno.WordPuzzle.entity.StdWord;
import com.hakno.WordPuzzle.repository.StdWordRepository;
import com.hakno.WordPuzzle.util.GridConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BacktrackingPuzzleGenerator 테스트")
class BacktrackingPuzzleGeneratorTest {

    @Mock
    private StdWordRepository stdWordRepository;

    private BacktrackingPuzzleGenerator generator;
    private PlacementValidator placementValidator;
    private GridConverter gridConverter;
    private PuzzleScorer puzzleScorer;

    @BeforeEach
    void setUp() {
        placementValidator = new PlacementValidator();
        gridConverter = new GridConverter();
        puzzleScorer = new PuzzleScorer();
        generator = new BacktrackingPuzzleGenerator(
                stdWordRepository, placementValidator, gridConverter, puzzleScorer);
    }

    @Nested
    @DisplayName("기본 퍼즐 생성 테스트")
    class BasicGenerationTests {

        @Test
        @DisplayName("첫 번째 단어 배치 성공")
        void generate_placesFirstWord() {
            // given
            List<StdWord> words = createMockWords(List.of("테스트", "사과", "바나나"));
            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(words);
            when(stdWordRepository.findWordsContainingCharWithSenses(anyString(), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(new ArrayList<>());

            // when
            PuzzleResponse result = generator.generate(10, 5);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalWords()).isGreaterThanOrEqualTo(1);
            assertThat(result.getGridSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("데이터가 없으면 예외 발생")
        void generate_noData_throwsException() {
            // given
            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(new ArrayList<>());

            // when & then
            assertThatThrownBy(() -> generator.generate(10, 5))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("조건에 맞는 단어가 없습니다");
        }

        @Test
        @DisplayName("여러 단어 배치 시도")
        void generate_multipleWords() {
            // given
            List<StdWord> firstWords = createMockWords(List.of("컴퓨터"));
            List<StdWord> intersectingWords = createMockWords(List.of("모퓨터", "퓨전", "컵라면"));

            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(firstWords);
            when(stdWordRepository.findWordsContainingCharWithSenses(anyString(), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(intersectingWords);

            // when
            PuzzleResponse result = generator.generate(12, 5, null, null, 2000);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getGrid()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("타임아웃 테스트")
    class TimeoutTests {

        @Test
        @DisplayName("타임아웃 내에 결과 반환")
        void generate_withinTimeout() {
            // given
            List<StdWord> words = createMockWords(List.of("테스트"));
            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(words);
            when(stdWordRepository.findWordsContainingCharWithSenses(anyString(), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(new ArrayList<>());

            long startTime = System.currentTimeMillis();

            // when
            PuzzleResponse result = generator.generate(10, 5, null, null, 1000);

            // then
            long elapsed = System.currentTimeMillis() - startTime;
            assertThat(result).isNotNull();
            assertThat(elapsed).isLessThan(2000); // 타임아웃 + 여유시간
        }

        @Test
        @DisplayName("짧은 타임아웃에도 최선의 결과 반환")
        void generate_shortTimeout_returnsBestResult() {
            // given
            List<StdWord> words = createMockWords(List.of("테스트", "사과"));
            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(words);
            when(stdWordRepository.findWordsContainingCharWithSenses(anyString(), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(new ArrayList<>());

            // when - 매우 짧은 타임아웃
            PuzzleResponse result = generator.generate(10, 10, null, null, 100);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalWords()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("카테고리/단어유형 필터 테스트")
    class FilterTests {

        @Test
        @DisplayName("카테고리 필터 적용")
        void generate_withCategory() {
            // given
            List<StdWord> words = createMockWords(List.of("의학용어"));
            when(stdWordRepository.findRandomWordsByCategory(eq("의학"), anyInt(), anyInt(), anyInt()))
                    .thenReturn(words);
            when(stdWordRepository.findRandomWordsByCategoryContainingChar(eq("의학"), anyString(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>());

            // when
            PuzzleResponse result = generator.generate(10, 5, "의학", null, 1000);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalWords()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("단어유형 필터 적용")
        void generate_withWordType() {
            // given
            List<StdWord> words = createMockWords(List.of("고유어단어"));
            when(stdWordRepository.findRandomWordsByWordType(eq("고유어"), anyInt(), anyInt(), anyInt()))
                    .thenReturn(words);
            when(stdWordRepository.findRandomWordsByWordTypeContainingChar(eq("고유어"), anyString(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>());

            // when
            PuzzleResponse result = generator.generate(10, 5, null, "고유어", 1000);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("퍼즐 응답 구조 테스트")
    class ResponseStructureTests {

        @Test
        @DisplayName("응답에 필수 필드 포함")
        void generate_responseHasRequiredFields() {
            // given
            List<StdWord> words = createMockWords(List.of("테스트"));
            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(words);
            when(stdWordRepository.findWordsContainingCharWithSenses(anyString(), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(new ArrayList<>());

            // when
            PuzzleResponse result = generator.generate(10, 5);

            // then
            assertThat(result.getGridSize()).isEqualTo(10);
            assertThat(result.getGrid()).isNotNull();
            assertThat(result.getGrid()).hasSize(10);
            assertThat(result.getAcrossWords()).isNotNull();
            assertThat(result.getDownWords()).isNotNull();
            assertThat(result.getTotalWords()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("가로 단어에 번호 순서대로 부여")
        void generate_acrossWordsNumberedCorrectly() {
            // given
            List<StdWord> words = createMockWords(List.of("가나다라"));
            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(words);
            when(stdWordRepository.findWordsContainingCharWithSenses(anyString(), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(new ArrayList<>());

            // when
            PuzzleResponse result = generator.generate(10, 3);

            // then
            if (!result.getAcrossWords().isEmpty()) {
                assertThat(result.getAcrossWords().get(0).getNumber()).isEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("병렬 생성 테스트")
    class ParallelGenerationTests {

        @Test
        @DisplayName("병렬 생성으로 퍼즐 생성")
        void generateParallel_createsMultiplePuzzles() {
            // given
            List<StdWord> words = createMockWords(List.of("가나다", "라마바", "사아자"));
            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(words);
            when(stdWordRepository.findWordsContainingCharWithSenses(anyString(), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(new ArrayList<>());

            // when
            PuzzleResponse result = generator.generateParallel(10, 5, null, null, 3000, 3);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalWords()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("병렬 생성 기본 파라미터")
        void generateParallel_defaultParams() {
            // given
            List<StdWord> words = createMockWords(List.of("테스트"));
            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(words);
            when(stdWordRepository.findWordsContainingCharWithSenses(anyString(), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(new ArrayList<>());

            // when
            PuzzleResponse result = generator.generateParallel(10, 5);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("시드 단어가 없으면 예외 발생")
        void generateParallel_noSeeds_throwsException() {
            // given
            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(new ArrayList<>());

            // when & then
            assertThatThrownBy(() -> generator.generateParallel(10, 5))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("시드 단어가 없습니다");
        }

        @Test
        @DisplayName("병렬 생성 타임아웃 내에 결과 반환")
        void generateParallel_withinTimeout() {
            // given
            List<StdWord> words = createMockWords(List.of("가나다", "라마바"));
            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(words);
            when(stdWordRepository.findWordsContainingCharWithSenses(anyString(), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(new ArrayList<>());

            long startTime = System.currentTimeMillis();

            // when
            PuzzleResponse result = generator.generateParallel(10, 5, null, null, 2000, 2);

            // then
            long elapsed = System.currentTimeMillis() - startTime;
            assertThat(result).isNotNull();
            assertThat(elapsed).isLessThan(4000); // 타임아웃 + 여유시간
        }
    }

    @Nested
    @DisplayName("교차 배치 테스트")
    class IntersectionTests {

        @Test
        @DisplayName("교차 가능한 단어 배치 시도")
        void generate_attemptsIntersections() {
            // given - 교차 가능한 단어들 설정
            List<StdWord> firstWords = createMockWords(List.of("가나다"));  // 첫 단어
            List<StdWord> intersectingWords = createMockWords(List.of("나라", "라면", "나무"));  // '나' 포함 단어들

            when(stdWordRepository.findRandomWordsWithSenses(anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(firstWords);
            // anyString()으로 모든 글자에 대해 stub 설정
            when(stdWordRepository.findWordsContainingCharWithSenses(anyString(), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(intersectingWords);

            // when
            PuzzleResponse result = generator.generate(12, 3, null, null, 3000);

            // then
            assertThat(result).isNotNull();
            // 백트래킹으로 교차 시도했는지 확인 (단어 수가 1보다 클 수 있음)
            assertThat(result.getTotalWords()).isGreaterThanOrEqualTo(1);
        }
    }

    // ============== 헬퍼 메서드 ==============

    private List<StdWord> createMockWords(List<String> wordStrings) {
        List<StdWord> words = new ArrayList<>();
        for (int i = 0; i < wordStrings.size(); i++) {
            String wordStr = wordStrings.get(i);

            StdSense sense = StdSense.builder()
                    .senseCode("SENSE" + i)
                    .senseOrder(1)
                    .definition(wordStr + "의 뜻풀이")
                    .build();

            StdWord word = StdWord.builder()
                    .targetCode("TEST" + i)
                    .word(wordStr)
                    .build();

            word.addSense(sense);
            words.add(word);
        }
        return words;
    }
}

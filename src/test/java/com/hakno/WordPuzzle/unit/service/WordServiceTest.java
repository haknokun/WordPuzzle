package com.hakno.WordPuzzle.unit.service;

import com.hakno.WordPuzzle.dto.WordDto;
import com.hakno.WordPuzzle.entity.Definition;
import com.hakno.WordPuzzle.entity.Word;
import com.hakno.WordPuzzle.repository.WordRepository;
import com.hakno.WordPuzzle.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * WordService 단위 테스트
 * Task 6: Backend Service 패키지 커버리지 개선
 */
@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    @Mock
    private WordRepository wordRepository;

    @InjectMocks
    private WordService wordService;

    private Word testWord;

    @BeforeEach
    void setUp() {
        testWord = createTestWord("사과", "명사", "초급");
    }

    private Word createTestWord(String wordText, String partOfSpeech, String level) {
        Word word = Word.builder()
                .word(wordText)
                .partOfSpeech(partOfSpeech)
                .vocabularyLevel(level)
                .build();

        Definition def = Definition.builder()
                .senseOrder(1)
                .definition("테스트 정의")
                .build();
        word.addDefinition(def);

        return word;
    }

    @Nested
    @DisplayName("findById 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("존재하는 ID로 조회 시 WordDto 반환")
        void shouldReturnWordDtoWhenIdExists() {
            // Given
            when(wordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            // When
            Optional<WordDto> result = wordService.findById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getWord()).isEqualTo("사과");
            assertThat(result.get().getLength()).isEqualTo(2);
            verify(wordRepository).findById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional 반환")
        void shouldReturnEmptyWhenIdNotExists() {
            // Given
            when(wordRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<WordDto> result = wordService.findById(999L);

            // Then
            assertThat(result).isEmpty();
            verify(wordRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("findByWord 테스트")
    class FindByWordTest {

        @Test
        @DisplayName("존재하는 단어로 조회 시 WordDto 반환")
        void shouldReturnWordDtoWhenWordExists() {
            // Given
            when(wordRepository.findByWord("사과")).thenReturn(Optional.of(testWord));

            // When
            Optional<WordDto> result = wordService.findByWord("사과");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getWord()).isEqualTo("사과");
            verify(wordRepository).findByWord("사과");
        }

        @Test
        @DisplayName("존재하지 않는 단어로 조회 시 빈 Optional 반환")
        void shouldReturnEmptyWhenWordNotExists() {
            // Given
            when(wordRepository.findByWord("없는단어")).thenReturn(Optional.empty());

            // When
            Optional<WordDto> result = wordService.findByWord("없는단어");

            // Then
            assertThat(result).isEmpty();
            verify(wordRepository).findByWord("없는단어");
        }
    }

    @Nested
    @DisplayName("findByLength 테스트")
    class FindByLengthTest {

        @Test
        @DisplayName("해당 길이의 단어들 반환")
        void shouldReturnWordsWithSpecifiedLength() {
            // Given
            Word word1 = createTestWord("사과", "명사", "초급");
            Word word2 = createTestWord("배추", "명사", "초급");
            when(wordRepository.findByLength(2)).thenReturn(List.of(word1, word2));

            // When
            List<WordDto> result = wordService.findByLength(2);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(WordDto::getWord).containsExactly("사과", "배추");
            verify(wordRepository).findByLength(2);
        }

        @Test
        @DisplayName("해당 길이의 단어가 없으면 빈 리스트 반환")
        void shouldReturnEmptyListWhenNoWordsWithLength() {
            // Given
            when(wordRepository.findByLength(10)).thenReturn(Collections.emptyList());

            // When
            List<WordDto> result = wordService.findByLength(10);

            // Then
            assertThat(result).isEmpty();
            verify(wordRepository).findByLength(10);
        }
    }

    @Nested
    @DisplayName("findRandomByLength 테스트")
    class FindRandomByLengthTest {

        @Test
        @DisplayName("지정된 길이의 랜덤 단어들을 limit만큼 반환")
        void shouldReturnLimitedRandomWords() {
            // Given
            Word word1 = createTestWord("사과", "명사", "초급");
            Word word2 = createTestWord("배추", "명사", "초급");
            Word word3 = createTestWord("당근", "명사", "중급");
            when(wordRepository.findRandomByLength(2)).thenReturn(List.of(word1, word2, word3));

            // When
            List<WordDto> result = wordService.findRandomByLength(2, 2);

            // Then
            assertThat(result).hasSize(2);
            verify(wordRepository).findRandomByLength(2);
        }

        @Test
        @DisplayName("limit보다 적은 단어가 있으면 모두 반환")
        void shouldReturnAllWordsWhenLessThanLimit() {
            // Given
            Word word1 = createTestWord("사과", "명사", "초급");
            when(wordRepository.findRandomByLength(2)).thenReturn(List.of(word1));

            // When
            List<WordDto> result = wordService.findRandomByLength(2, 5);

            // Then
            assertThat(result).hasSize(1);
            verify(wordRepository).findRandomByLength(2);
        }

        @Test
        @DisplayName("단어가 없으면 빈 리스트 반환")
        void shouldReturnEmptyListWhenNoWords() {
            // Given
            when(wordRepository.findRandomByLength(anyInt())).thenReturn(Collections.emptyList());

            // When
            List<WordDto> result = wordService.findRandomByLength(10, 5);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("count 테스트")
    class CountTest {

        @Test
        @DisplayName("전체 단어 수 반환")
        void shouldReturnTotalWordCount() {
            // Given
            when(wordRepository.count()).thenReturn(1000L);

            // When
            long result = wordService.count();

            // Then
            assertThat(result).isEqualTo(1000L);
            verify(wordRepository).count();
        }

        @Test
        @DisplayName("단어가 없으면 0 반환")
        void shouldReturnZeroWhenNoWords() {
            // Given
            when(wordRepository.count()).thenReturn(0L);

            // When
            long result = wordService.count();

            // Then
            assertThat(result).isZero();
            verify(wordRepository).count();
        }
    }
}

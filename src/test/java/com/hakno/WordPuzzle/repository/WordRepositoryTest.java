package com.hakno.WordPuzzle.repository;

import com.hakno.WordPuzzle.entity.Definition;
import com.hakno.WordPuzzle.entity.Word;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WordRepository 통합 테스트
 * H2 인메모리 DB를 사용하여 기본 JPA 쿼리 동작을 검증합니다.
 *
 * Note: RAND() 함수를 사용하는 메서드는 H2에서 지원되지 않아
 * MySQL 환경에서 별도로 테스트해야 합니다.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("WordRepository 통합 테스트")
class WordRepositoryTest {

    @Autowired
    private WordRepository wordRepository;

    @BeforeEach
    void setUp() {
        wordRepository.deleteAll();

        // 테스트 데이터 생성
        createWordWithDefinition("사과", "명사", "초급", "나무에서 열리는 빨간 과일");
        createWordWithDefinition("바나나", "명사", "초급", "노란색의 긴 열대 과일");
        createWordWithDefinition("포도", "명사", "초급", "보라색의 작은 과일 송이");
        createWordWithDefinition("컴퓨터", "명사", "중급", "전자 계산 장치");
        createWordWithDefinition("프로그래밍", "명사", "고급", "컴퓨터 프로그램을 작성하는 것");
        createWordWithDefinition("알고리즘", "명사", "고급", "문제 해결 절차");
        createWordWithDefinition("가나다", "명사", "초급", "한글 자모순");
        createWordWithDefinition("나비", "명사", "초급", "아름다운 날개를 가진 곤충");
    }

    private Word createWordWithDefinition(String wordText, String pos, String level, String def) {
        Word word = Word.builder()
                .word(wordText)
                .partOfSpeech(pos)
                .vocabularyLevel(level)
                .build();

        Definition definition = Definition.builder()
                .senseOrder(1)
                .definition(def)
                .build();
        word.addDefinition(definition);

        return wordRepository.save(word);
    }

    @Nested
    @DisplayName("기본 조회 메서드")
    class BasicQueryTests {

        @Test
        @DisplayName("단어로 검색 - 존재하는 단어")
        void findByWord_existingWord_returnsWord() {
            // when
            Optional<Word> result = wordRepository.findByWord("사과");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getWord()).isEqualTo("사과");
            assertThat(result.get().getLength()).isEqualTo(2);
            assertThat(result.get().getFirstChar()).isEqualTo("사");
        }

        @Test
        @DisplayName("단어로 검색 - 존재하지 않는 단어")
        void findByWord_nonExistingWord_returnsEmpty() {
            // when
            Optional<Word> result = wordRepository.findByWord("없는단어");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("길이로 검색")
        void findByLength_returnsMatchingWords() {
            // when
            List<Word> result = wordRepository.findByLength(2);

            // then
            assertThat(result).hasSize(3); // 사과, 포도, 나비
            assertThat(result).extracting(Word::getWord)
                    .containsExactlyInAnyOrder("사과", "포도", "나비");
        }

        @Test
        @DisplayName("길이 범위로 검색")
        void findByLengthBetween_returnsMatchingWords() {
            // when
            List<Word> result = wordRepository.findByLengthBetween(2, 3);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(w -> w.getLength() >= 2 && w.getLength() <= 3);
        }

        @Test
        @DisplayName("단어 존재 여부 확인 - 존재하는 단어")
        void existsByWord_existingWord_returnsTrue() {
            // when & then
            assertThat(wordRepository.existsByWord("컴퓨터")).isTrue();
        }

        @Test
        @DisplayName("단어 존재 여부 확인 - 존재하지 않는 단어")
        void existsByWord_nonExistingWord_returnsFalse() {
            // when & then
            assertThat(wordRepository.existsByWord("없는단어")).isFalse();
        }
    }

    @Nested
    @DisplayName("Word 엔티티 검증")
    class EntityValidationTests {

        @Test
        @DisplayName("Word 저장 시 length와 firstChar 자동 설정")
        void save_setsLengthAndFirstChar() {
            // when
            Word saved = wordRepository.save(
                    Word.builder()
                            .word("테스트")
                            .partOfSpeech("명사")
                            .vocabularyLevel("초급")
                            .build()
            );

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getLength()).isEqualTo(3);
            assertThat(saved.getFirstChar()).isEqualTo("테");
        }

        @Test
        @DisplayName("Definition과 함께 저장")
        void save_withDefinition() {
            // given
            Word word = Word.builder()
                    .word("새단어")
                    .partOfSpeech("명사")
                    .vocabularyLevel("초급")
                    .build();

            Definition definition = Definition.builder()
                    .senseOrder(1)
                    .definition("새 단어의 정의")
                    .build();
            word.addDefinition(definition);

            // when
            Word saved = wordRepository.save(word);

            // then
            assertThat(saved.getDefinitions()).hasSize(1);
            assertThat(saved.getDefinitions().get(0).getDefinition())
                    .isEqualTo("새 단어의 정의");
        }

        @Test
        @DisplayName("여러 Definition 저장")
        void save_withMultipleDefinitions() {
            // given
            Word word = Word.builder()
                    .word("다의어")
                    .partOfSpeech("명사")
                    .vocabularyLevel("중급")
                    .build();

            word.addDefinition(Definition.builder()
                    .senseOrder(1)
                    .definition("첫 번째 의미")
                    .build());
            word.addDefinition(Definition.builder()
                    .senseOrder(2)
                    .definition("두 번째 의미")
                    .build());

            // when
            Word saved = wordRepository.save(word);

            // then
            assertThat(saved.getDefinitions()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("난이도별 필터링")
    class VocabularyLevelTests {

        @Test
        @DisplayName("초급 단어 필터링")
        void filterByLevel_beginner() {
            // when
            List<Word> allWords = wordRepository.findAll();
            List<Word> beginnerWords = allWords.stream()
                    .filter(w -> "초급".equals(w.getVocabularyLevel()))
                    .toList();

            // then
            assertThat(beginnerWords).isNotEmpty();
            assertThat(beginnerWords).allMatch(w -> "초급".equals(w.getVocabularyLevel()));
        }

        @Test
        @DisplayName("고급 단어 필터링")
        void filterByLevel_advanced() {
            // when
            List<Word> allWords = wordRepository.findAll();
            List<Word> advancedWords = allWords.stream()
                    .filter(w -> "고급".equals(w.getVocabularyLevel()))
                    .toList();

            // then
            assertThat(advancedWords).hasSize(2); // 프로그래밍, 알고리즘
        }
    }

    @Nested
    @DisplayName("품사별 필터링")
    class PartOfSpeechTests {

        @Test
        @DisplayName("명사 필터링")
        void filterByPartOfSpeech_noun() {
            // when
            List<Word> allWords = wordRepository.findAll();
            List<Word> nouns = allWords.stream()
                    .filter(w -> "명사".equals(w.getPartOfSpeech()))
                    .toList();

            // then
            assertThat(nouns).hasSize(8); // 모든 테스트 단어가 명사
        }
    }

    @Nested
    @DisplayName("전체 조회")
    class FindAllTests {

        @Test
        @DisplayName("전체 단어 수 확인")
        void findAll_returnsAllWords() {
            // when
            List<Word> result = wordRepository.findAll();

            // then
            assertThat(result).hasSize(8);
        }

        @Test
        @DisplayName("삭제 후 전체 조회")
        void findAll_afterDelete() {
            // given
            Optional<Word> toDelete = wordRepository.findByWord("사과");
            assertThat(toDelete).isPresent();
            wordRepository.delete(toDelete.get());

            // when
            List<Word> result = wordRepository.findAll();

            // then
            assertThat(result).hasSize(7);
            assertThat(result).noneMatch(w -> "사과".equals(w.getWord()));
        }
    }
}

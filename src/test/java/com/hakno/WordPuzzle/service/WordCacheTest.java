package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.entity.StdSense;
import com.hakno.WordPuzzle.entity.StdWord;
import com.hakno.WordPuzzle.repository.StdWordRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WordCache 테스트")
class WordCacheTest {

    @Mock
    private StdWordRepository stdWordRepository;

    private WordCache wordCache;

    @BeforeEach
    void setUp() {
        wordCache = new WordCache(stdWordRepository);
        wordCache.clear(); // 각 테스트 전에 캐시 클리어
    }

    @Nested
    @DisplayName("캐시 초기화 테스트")
    class InitializationTests {

        @Test
        @DisplayName("캐시 통계 확인")
        void getStats_returnsStats() {
            // when
            WordCache.CacheStats stats = wordCache.getStats();

            // then
            assertThat(stats).isNotNull();
            assertThat(stats.wordCount()).isEqualTo(0);
            assertThat(stats.initialized()).isFalse();
        }

        @Test
        @DisplayName("캐시 클리어")
        void clear_clearsAllData() {
            // when
            wordCache.clear();

            // then
            assertThat(wordCache.getStats().wordCount()).isEqualTo(0);
            assertThat(wordCache.getStats().initialized()).isFalse();
        }
    }

    @Nested
    @DisplayName("단어 조회 테스트")
    class QueryTests {

        @Test
        @DisplayName("글자로 단어 조회 - 캐시 미스시 DB 조회")
        void getWordsContainingChar_cacheMiss_queriesDb() {
            // given
            List<StdWord> mockWords = createMockWords(List.of("가나다", "가마바"));
            when(stdWordRepository.findWordsContainingCharWithSenses(eq("가"), anyInt(), anyInt(), any(PageRequest.class)))
                    .thenReturn(mockWords);

            // when
            List<StdWord> result = wordCache.getWordsContainingChar('가', 2, 5, 10);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("길이로 랜덤 단어 조회 - 캐시 미스시 DB 조회")
        void getRandomWordsByLength_cacheMiss_queriesDb() {
            // given
            List<StdWord> mockWords = createMockWords(List.of("가나", "다라"));
            when(stdWordRepository.findRandomWordsWithSenses(eq(2), eq(3), any(PageRequest.class)))
                    .thenReturn(mockWords);

            // when
            List<StdWord> result = wordCache.getRandomWordsByLength(2, 3, 10);

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("캐시 통계 테스트")
    class StatsTests {

        @Test
        @DisplayName("통계 toString 형식")
        void stats_toString() {
            // when
            WordCache.CacheStats stats = wordCache.getStats();
            String str = stats.toString();

            // then
            assertThat(str).contains("WordCache");
            assertThat(str).contains("words=");
            assertThat(str).contains("initialized=");
        }
    }

    // ============== 헬퍼 메서드 ==============

    private List<StdWord> createMockWords(List<String> wordStrings) {
        List<StdWord> words = new ArrayList<>();
        long id = 1L;
        for (String wordStr : wordStrings) {
            StdSense sense = StdSense.builder()
                    .senseCode("SENSE" + id)
                    .senseOrder(1)
                    .definition(wordStr + "의 뜻풀이")
                    .build();

            StdWord word = StdWord.builder()
                    .targetCode("TEST" + id)
                    .word(wordStr)
                    .build();

            // ID 설정을 위해 reflection 사용 (테스트용)
            try {
                java.lang.reflect.Field idField = StdWord.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(word, id++);
            } catch (Exception e) {
                // 무시
            }

            word.addSense(sense);
            words.add(word);
        }
        return words;
    }
}

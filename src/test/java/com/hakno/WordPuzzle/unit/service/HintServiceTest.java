package com.hakno.WordPuzzle.unit.service;

import com.hakno.WordPuzzle.dto.HintResponse;
import com.hakno.WordPuzzle.dto.HintType;
import com.hakno.WordPuzzle.entity.StdExample;
import com.hakno.WordPuzzle.entity.StdSense;
import com.hakno.WordPuzzle.entity.StdWord;
import com.hakno.WordPuzzle.entity.StdWordRelation;
import com.hakno.WordPuzzle.repository.StdWordRepository;
import com.hakno.WordPuzzle.service.HintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * HintService 단위 테스트
 * Task 22: 힌트 시스템 확장
 */
@ExtendWith(MockitoExtension.class)
class HintServiceTest {

    @Mock
    private StdWordRepository stdWordRepository;

    @InjectMocks
    private HintService hintService;

    private StdWord testWord;
    private StdSense testSense;

    @BeforeEach
    void setUp() {
        testWord = createTestWord();
        testSense = createTestSense();
        testWord.addSense(testSense);
    }

    private StdWord createTestWord() {
        return StdWord.builder()
                .targetCode("TEST001")
                .word("사과")
                .wordType("고유어")
                .origin("沙果")
                .pronunciation("사과")
                .build();
    }

    private StdSense createTestSense() {
        return StdSense.builder()
                .senseCode("SENSE001")
                .senseOrder(1)
                .pos("명사")
                .category("식물")
                .definition("사과나무의 열매")
                .type("일반어")
                .build();
    }

    @Nested
    @DisplayName("초성 힌트 테스트")
    class ChosungHintTest {

        @Test
        @DisplayName("한글 단어의 초성을 올바르게 추출한다")
        void extractChosungFromKoreanWord() {
            String chosung = hintService.extractChosung("사과");
            assertThat(chosung).isEqualTo("ㅅㄱ");
        }

        @Test
        @DisplayName("여러 글자 단어의 초성을 추출한다")
        void extractChosungFromLongWord() {
            String chosung = hintService.extractChosung("대한민국");
            assertThat(chosung).isEqualTo("ㄷㅎㅁㄱ");
        }

        @Test
        @DisplayName("쌍자음 초성도 올바르게 추출한다")
        void extractDoubleConsonantChosung() {
            String chosung = hintService.extractChosung("빵");
            assertThat(chosung).isEqualTo("ㅃ");
        }

        @Test
        @DisplayName("비한글 문자는 그대로 유지한다")
        void preserveNonKoreanCharacters() {
            String chosung = hintService.extractChosung("A사과B");
            assertThat(chosung).isEqualTo("AㅅㄱB");
        }

        @Test
        @DisplayName("빈 문자열은 빈 문자열을 반환한다")
        void emptyStringReturnsEmpty() {
            String chosung = hintService.extractChosung("");
            assertThat(chosung).isEmpty();
        }

        @Test
        @DisplayName("null은 빈 문자열을 반환한다")
        void nullReturnsEmpty() {
            String chosung = hintService.extractChosung(null);
            assertThat(chosung).isEmpty();
        }

        @Test
        @DisplayName("getHint로 초성 힌트를 조회한다")
        void getChosungHint() {
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.CHOSUNG);

            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getHintType()).isEqualTo(HintType.CHOSUNG);
            assertThat(response.getValue()).isEqualTo("ㅅㄱ");
        }
    }

    @Nested
    @DisplayName("어원 힌트 테스트")
    class OriginHintTest {

        @Test
        @DisplayName("어원 정보가 있으면 반환한다")
        void returnOriginWhenAvailable() {
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.ORIGIN);

            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getValue()).isEqualTo("沙果");
        }

        @Test
        @DisplayName("어원 정보가 없으면 사용 불가 응답을 반환한다")
        void returnNotAvailableWhenNoOrigin() {
            StdWord wordWithoutOrigin = StdWord.builder()
                    .targetCode("TEST002")
                    .word("테스트")
                    .build();
            when(stdWordRepository.findById(2L)).thenReturn(Optional.of(wordWithoutOrigin));

            HintResponse response = hintService.getHint(2L, HintType.ORIGIN);

            assertThat(response.isAvailable()).isFalse();
            assertThat(response.getMessage()).contains("어원 정보가 없습니다");
        }
    }

    @Nested
    @DisplayName("발음 힌트 테스트")
    class PronunciationHintTest {

        @Test
        @DisplayName("발음 정보가 있으면 반환한다")
        void returnPronunciationWhenAvailable() {
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.PRONUNCIATION);

            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getValue()).isEqualTo("사과");
        }
    }

    @Nested
    @DisplayName("단어유형 힌트 테스트")
    class WordTypeHintTest {

        @Test
        @DisplayName("단어유형 정보가 있으면 반환한다")
        void returnWordTypeWhenAvailable() {
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.WORD_TYPE);

            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getValue()).isEqualTo("고유어");
        }
    }

    @Nested
    @DisplayName("품사 힌트 테스트")
    class PosHintTest {

        @Test
        @DisplayName("품사 정보가 있으면 반환한다")
        void returnPosWhenAvailable() {
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.POS);

            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getValue()).isEqualTo("명사");
        }
    }

    @Nested
    @DisplayName("분야 힌트 테스트")
    class CategoryHintTest {

        @Test
        @DisplayName("분야 정보가 있으면 반환한다")
        void returnCategoryWhenAvailable() {
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.CATEGORY);

            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getValues()).contains("식물");
        }
    }

    @Nested
    @DisplayName("예문 힌트 테스트")
    class ExampleHintTest {

        @Test
        @DisplayName("예문이 있으면 반환한다")
        void returnExamplesWhenAvailable() {
            StdExample example = StdExample.builder()
                    .example("빨간 사과가 맛있다.")
                    .build();
            testSense.addExample(example);
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.EXAMPLE);

            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getValues()).contains("빨간 사과가 맛있다.");
        }

        @Test
        @DisplayName("예문이 없으면 사용 불가 응답을 반환한다")
        void returnNotAvailableWhenNoExamples() {
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.EXAMPLE);

            assertThat(response.isAvailable()).isFalse();
            assertThat(response.getMessage()).contains("예문이 없습니다");
        }
    }

    @Nested
    @DisplayName("유의어/반의어 힌트 테스트")
    class RelationHintTest {

        @Test
        @DisplayName("유의어가 있으면 반환한다")
        void returnSynonymsWhenAvailable() {
            StdWordRelation synonym = StdWordRelation.builder()
                    .relationType("비슷한말")
                    .relatedWord("능금")
                    .build();
            testSense.addRelation(synonym);
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.SYNONYM);

            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getValues()).contains("능금");
        }

        @Test
        @DisplayName("유의어가 없으면 사용 불가 응답을 반환한다")
        void returnNotAvailableWhenNoSynonyms() {
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.SYNONYM);

            assertThat(response.isAvailable()).isFalse();
            assertThat(response.getMessage()).contains("유의어가 없습니다");
        }

        @Test
        @DisplayName("반의어가 있으면 반환한다")
        void returnAntonymsWhenAvailable() {
            StdWordRelation antonym = StdWordRelation.builder()
                    .relationType("반대말")
                    .relatedWord("배")
                    .build();
            testSense.addRelation(antonym);
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            HintResponse response = hintService.getHint(1L, HintType.ANTONYM);

            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getValues()).contains("배");
        }
    }

    @Nested
    @DisplayName("힌트 조회 공통 테스트")
    class CommonHintTest {

        @Test
        @DisplayName("존재하지 않는 단어 ID로 조회하면 사용 불가 응답을 반환한다")
        void returnNotAvailableForNonExistentWord() {
            when(stdWordRepository.findById(999L)).thenReturn(Optional.empty());

            HintResponse response = hintService.getHint(999L, HintType.CHOSUNG);

            assertThat(response.isAvailable()).isFalse();
            assertThat(response.getMessage()).contains("단어를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("모든 힌트를 조회한다")
        void getAllHints() {
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            List<HintResponse> hints = hintService.getAllHints(1L);

            assertThat(hints).hasSize(HintType.values().length);
        }

        @Test
        @DisplayName("사용 가능한 힌트만 조회한다")
        void getAvailableHints() {
            when(stdWordRepository.findById(1L)).thenReturn(Optional.of(testWord));

            List<HintResponse> hints = hintService.getAvailableHints(1L);

            assertThat(hints).allMatch(HintResponse::isAvailable);
            assertThat(hints.size()).isLessThanOrEqualTo(HintType.values().length);
        }

        @Test
        @DisplayName("targetCode로 힌트를 조회한다")
        void getHintByTargetCode() {
            // testWord에 ID가 설정되어 있지 않으므로, findById도 Mock 설정 필요
            when(stdWordRepository.findByTargetCode("TEST001")).thenReturn(Optional.of(testWord));
            // getHintByTargetCode는 내부적으로 getHint(word.getId(), hintType)을 호출하지 않고
            // 직접 word 객체를 사용하므로, findById Mock은 필요 없음
            // 대신 testWord가 반환되면 extractChosung이 호출됨

            HintResponse response = hintService.getHintByTargetCode("TEST001", HintType.CHOSUNG);

            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getValue()).isEqualTo("ㅅㄱ");
        }
    }
}

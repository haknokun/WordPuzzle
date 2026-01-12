package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.dto.HintResponse;
import com.hakno.WordPuzzle.dto.HintType;
import com.hakno.WordPuzzle.entity.StdExample;
import com.hakno.WordPuzzle.entity.StdSense;
import com.hakno.WordPuzzle.entity.StdWord;
import com.hakno.WordPuzzle.entity.StdWordRelation;
import com.hakno.WordPuzzle.repository.StdWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 힌트 서비스
 * 다양한 유형의 힌트를 제공하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HintService {

    private final StdWordRepository stdWordRepository;

    // 한글 초성 목록
    private static final char[] CHOSUNG_LIST = {
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
        'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    // 한글 유니코드 범위
    private static final int HANGUL_SYLLABLE_START = 0xAC00; // '가'
    private static final int HANGUL_SYLLABLE_END = 0xD7A3;   // '힣'
    private static final int SYLLABLE_PER_CHOSUNG = 588;     // 초성당 음절 수 (21 * 28)

    /**
     * 힌트 조회 (메인 메서드)
     *
     * @param wordId   단어 ID
     * @param hintType 힌트 타입
     * @return 힌트 응답
     */
    public HintResponse getHint(Long wordId, HintType hintType) {
        StdWord word = stdWordRepository.findById(wordId)
                .orElse(null);

        if (word == null) {
            return HintResponse.notAvailable(hintType, wordId, null, "단어를 찾을 수 없습니다.");
        }

        return switch (hintType) {
            case CHOSUNG -> getChosungHint(word);
            case EXAMPLE -> getExampleHint(word);
            case ORIGIN -> getOriginHint(word);
            case PRONUNCIATION -> getPronunciationHint(word);
            case SYNONYM -> getSynonymHint(word);
            case ANTONYM -> getAntonymHint(word);
            case CATEGORY -> getCategoryHint(word);
            case POS -> getPosHint(word);
            case WORD_TYPE -> getWordTypeHint(word);
        };
    }

    /**
     * 단어로 힌트 조회 (targetCode 사용)
     */
    public HintResponse getHintByTargetCode(String targetCode, HintType hintType) {
        StdWord word = stdWordRepository.findByTargetCode(targetCode)
                .orElse(null);

        if (word == null) {
            return HintResponse.notAvailable(hintType, null, null, "단어를 찾을 수 없습니다.");
        }

        // 이미 word 객체를 가지고 있으므로 직접 힌트 조회
        return getHintForWord(word, hintType);
    }

    /**
     * StdWord 객체로 직접 힌트 조회 (내부 메서드)
     */
    private HintResponse getHintForWord(StdWord word, HintType hintType) {
        return switch (hintType) {
            case CHOSUNG -> getChosungHint(word);
            case EXAMPLE -> getExampleHint(word);
            case ORIGIN -> getOriginHint(word);
            case PRONUNCIATION -> getPronunciationHint(word);
            case SYNONYM -> getSynonymHint(word);
            case ANTONYM -> getAntonymHint(word);
            case CATEGORY -> getCategoryHint(word);
            case POS -> getPosHint(word);
            case WORD_TYPE -> getWordTypeHint(word);
        };
    }

    /**
     * 모든 타입의 힌트 조회
     */
    public List<HintResponse> getAllHints(Long wordId) {
        List<HintResponse> hints = new ArrayList<>();
        for (HintType type : HintType.values()) {
            hints.add(getHint(wordId, type));
        }
        return hints;
    }

    /**
     * 사용 가능한 힌트만 조회
     */
    public List<HintResponse> getAvailableHints(Long wordId) {
        return getAllHints(wordId).stream()
                .filter(HintResponse::isAvailable)
                .collect(Collectors.toList());
    }

    // ==================== 개별 힌트 메서드 ====================

    /**
     * 초성 힌트 - 단어의 초성만 반환
     */
    public HintResponse getChosungHint(StdWord word) {
        String chosung = extractChosung(word.getWord());
        return HintResponse.ofSingle(HintType.CHOSUNG, word.getId(), word.getWord(), chosung);
    }

    /**
     * 예문 힌트 - 단어가 사용된 예문 목록 반환
     */
    public HintResponse getExampleHint(StdWord word) {
        List<String> examples = word.getSenses().stream()
                .flatMap(sense -> sense.getExamples().stream())
                .map(StdExample::getExample)
                .filter(example -> example != null && !example.isEmpty())
                .limit(5)  // 최대 5개
                .collect(Collectors.toList());

        if (examples.isEmpty()) {
            return HintResponse.notAvailable(HintType.EXAMPLE, word.getId(), word.getWord(),
                    "예문이 없습니다.");
        }

        return HintResponse.ofMultiple(HintType.EXAMPLE, word.getId(), word.getWord(), examples);
    }

    /**
     * 어원 힌트 - 단어의 어원(한자 등) 정보 반환
     */
    public HintResponse getOriginHint(StdWord word) {
        String origin = word.getOrigin();

        if (origin == null || origin.isEmpty()) {
            return HintResponse.notAvailable(HintType.ORIGIN, word.getId(), word.getWord(),
                    "어원 정보가 없습니다.");
        }

        return HintResponse.ofSingle(HintType.ORIGIN, word.getId(), word.getWord(), origin);
    }

    /**
     * 발음 힌트 - 단어의 발음 정보 반환
     */
    public HintResponse getPronunciationHint(StdWord word) {
        String pronunciation = word.getPronunciation();

        if (pronunciation == null || pronunciation.isEmpty()) {
            return HintResponse.notAvailable(HintType.PRONUNCIATION, word.getId(), word.getWord(),
                    "발음 정보가 없습니다.");
        }

        return HintResponse.ofSingle(HintType.PRONUNCIATION, word.getId(), word.getWord(), pronunciation);
    }

    /**
     * 유의어 힌트 - 비슷한 의미의 단어 목록 반환
     */
    public HintResponse getSynonymHint(StdWord word) {
        List<String> synonyms = word.getSenses().stream()
                .flatMap(sense -> sense.getRelations().stream())
                .filter(relation -> "비슷한말".equals(relation.getRelationType()))
                .map(StdWordRelation::getRelatedWord)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        if (synonyms.isEmpty()) {
            return HintResponse.notAvailable(HintType.SYNONYM, word.getId(), word.getWord(),
                    "유의어가 없습니다.");
        }

        return HintResponse.ofMultiple(HintType.SYNONYM, word.getId(), word.getWord(), synonyms);
    }

    /**
     * 반의어 힌트 - 반대 의미의 단어 목록 반환
     */
    public HintResponse getAntonymHint(StdWord word) {
        List<String> antonyms = word.getSenses().stream()
                .flatMap(sense -> sense.getRelations().stream())
                .filter(relation -> "반대말".equals(relation.getRelationType()))
                .map(StdWordRelation::getRelatedWord)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        if (antonyms.isEmpty()) {
            return HintResponse.notAvailable(HintType.ANTONYM, word.getId(), word.getWord(),
                    "반의어가 없습니다.");
        }

        return HintResponse.ofMultiple(HintType.ANTONYM, word.getId(), word.getWord(), antonyms);
    }

    /**
     * 분야 힌트 - 단어가 속한 전문 분야 목록 반환
     */
    public HintResponse getCategoryHint(StdWord word) {
        List<String> categories = word.getSenses().stream()
                .map(StdSense::getCategory)
                .filter(category -> category != null && !category.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (categories.isEmpty()) {
            return HintResponse.notAvailable(HintType.CATEGORY, word.getId(), word.getWord(),
                    "분야 정보가 없습니다.");
        }

        return HintResponse.ofMultiple(HintType.CATEGORY, word.getId(), word.getWord(), categories);
    }

    /**
     * 품사 힌트 - 단어의 품사 정보 반환
     */
    public HintResponse getPosHint(StdWord word) {
        List<String> posList = word.getSenses().stream()
                .map(StdSense::getPos)
                .filter(pos -> pos != null && !pos.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (posList.isEmpty()) {
            return HintResponse.notAvailable(HintType.POS, word.getId(), word.getWord(),
                    "품사 정보가 없습니다.");
        }

        // 하나의 품사만 있으면 단일 값으로 반환
        if (posList.size() == 1) {
            return HintResponse.ofSingle(HintType.POS, word.getId(), word.getWord(), posList.get(0));
        }

        return HintResponse.ofMultiple(HintType.POS, word.getId(), word.getWord(), posList);
    }

    /**
     * 단어유형 힌트 - 고유어/한자어/외래어/혼종어 정보 반환
     */
    public HintResponse getWordTypeHint(StdWord word) {
        String wordType = word.getWordType();

        if (wordType == null || wordType.isEmpty()) {
            return HintResponse.notAvailable(HintType.WORD_TYPE, word.getId(), word.getWord(),
                    "단어유형 정보가 없습니다.");
        }

        return HintResponse.ofSingle(HintType.WORD_TYPE, word.getId(), word.getWord(), wordType);
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 문자열에서 초성 추출
     *
     * @param word 원본 문자열
     * @return 초성 문자열
     */
    public String extractChosung(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (isKoreanSyllable(c)) {
                int chosungIndex = (c - HANGUL_SYLLABLE_START) / SYLLABLE_PER_CHOSUNG;
                result.append(CHOSUNG_LIST[chosungIndex]);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 문자가 한글 음절인지 확인
     */
    private boolean isKoreanSyllable(char c) {
        return c >= HANGUL_SYLLABLE_START && c <= HANGUL_SYLLABLE_END;
    }
}

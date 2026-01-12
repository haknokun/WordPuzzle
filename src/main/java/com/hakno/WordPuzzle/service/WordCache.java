package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.entity.StdWord;
import com.hakno.WordPuzzle.repository.StdWordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 퍼즐 생성 성능 최적화를 위한 단어 캐시
 * - 글자별 단어 맵: 특정 글자를 포함하는 단어들을 미리 인덱싱
 * - 길이별 단어 맵: 특정 길이의 단어들을 미리 분류
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WordCache {

    private final StdWordRepository stdWordRepository;

    // 글자 → 해당 글자를 포함하는 단어 ID 목록
    private final Map<Character, Set<Long>> charToWordIds = new ConcurrentHashMap<>();

    // 길이 → 해당 길이의 단어 ID 목록
    private final Map<Integer, Set<Long>> lengthToWordIds = new ConcurrentHashMap<>();

    // 단어 ID → StdWord 캐시
    private final Map<Long, StdWord> wordCache = new ConcurrentHashMap<>();

    // 캐시 초기화 상태
    private volatile boolean initialized = false;

    // 캐시할 최대 단어 수
    private static final int MAX_CACHE_SIZE = 10000;

    // 퍼즐에 자주 사용되는 한글 글자
    private static final String COMMON_CHARS = "가나다라마바사아자차카타파하" +
            "거너더러머버서어저처커터퍼허" +
            "고노도로모보소오조초코토포호" +
            "구누두루무부수우주추쿠투푸후" +
            "이기니디리미비시이지치키티피히";

    /**
     * 애플리케이션 시작 시 캐시 초기화 (비동기로 실행)
     */
    @PostConstruct
    public void init() {
        // 초기화는 별도 스레드에서 비동기로 실행
        new Thread(this::initializeCache).start();
    }

    /**
     * 캐시 초기화 (실제 데이터 로딩)
     */
    private void initializeCache() {
        if (initialized) return;

        try {
            log.info("단어 캐시 초기화 시작...");
            long startTime = System.currentTimeMillis();

            // 자주 사용되는 길이의 단어들 로드 (2~7글자)
            for (int length = 2; length <= 7; length++) {
                List<StdWord> words = stdWordRepository.findRandomWordsWithSenses(
                        length, length, PageRequest.of(0, MAX_CACHE_SIZE / 6));

                for (StdWord word : words) {
                    cacheWord(word);
                }
            }

            initialized = true;
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("단어 캐시 초기화 완료: {}개 단어, {}ms", wordCache.size(), elapsed);

        } catch (Exception e) {
            log.warn("단어 캐시 초기화 실패: {}", e.getMessage());
        }
    }

    /**
     * 단어를 캐시에 추가
     */
    private void cacheWord(StdWord word) {
        if (word == null || word.getId() == null) return;

        // 단어 캐시에 추가
        wordCache.put(word.getId(), word);

        // 길이별 인덱스에 추가
        lengthToWordIds.computeIfAbsent(word.getLength(), k -> ConcurrentHashMap.newKeySet())
                .add(word.getId());

        // 글자별 인덱스에 추가
        for (char c : word.getWord().toCharArray()) {
            charToWordIds.computeIfAbsent(c, k -> ConcurrentHashMap.newKeySet())
                    .add(word.getId());
        }
    }

    /**
     * 특정 글자를 포함하는 단어 목록 가져오기 (캐시 우선)
     */
    public List<StdWord> getWordsContainingChar(char c, int minLength, int maxLength, int limit) {
        Set<Long> wordIds = charToWordIds.get(c);

        if (wordIds == null || wordIds.isEmpty()) {
            // 캐시 미스: DB에서 직접 조회
            return stdWordRepository.findWordsContainingCharWithSenses(
                    String.valueOf(c), minLength, maxLength, PageRequest.of(0, limit));
        }

        // 캐시 히트: 길이 필터링 후 반환
        return wordIds.stream()
                .map(wordCache::get)
                .filter(Objects::nonNull)
                .filter(w -> w.getLength() >= minLength && w.getLength() <= maxLength)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 특정 길이의 랜덤 단어 목록 가져오기 (캐시 우선)
     */
    public List<StdWord> getRandomWordsByLength(int minLength, int maxLength, int limit) {
        List<StdWord> result = new ArrayList<>();

        for (int len = minLength; len <= maxLength; len++) {
            Set<Long> wordIds = lengthToWordIds.get(len);
            if (wordIds != null) {
                List<StdWord> words = wordIds.stream()
                        .map(wordCache::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // 셔플하여 랜덤성 확보
                Collections.shuffle(words);
                result.addAll(words);
            }
        }

        if (result.isEmpty()) {
            // 캐시 미스: DB에서 직접 조회
            return stdWordRepository.findRandomWordsWithSenses(minLength, maxLength, PageRequest.of(0, limit));
        }

        Collections.shuffle(result);
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 캐시 상태 정보
     */
    public CacheStats getStats() {
        return new CacheStats(
                wordCache.size(),
                charToWordIds.size(),
                lengthToWordIds.size(),
                initialized
        );
    }

    /**
     * 캐시 강제 초기화 (테스트용)
     */
    public void forceInitialize() {
        initialized = false;
        initializeCache();
    }

    /**
     * 캐시 클리어 (테스트용)
     */
    public void clear() {
        wordCache.clear();
        charToWordIds.clear();
        lengthToWordIds.clear();
        initialized = false;
    }

    /**
     * 캐시 통계 정보
     */
    public record CacheStats(
            int wordCount,
            int charIndexCount,
            int lengthIndexCount,
            boolean initialized
    ) {
        @Override
        public String toString() {
            return String.format("WordCache[words=%d, chars=%d, lengths=%d, initialized=%s]",
                    wordCount, charIndexCount, lengthIndexCount, initialized);
        }
    }
}

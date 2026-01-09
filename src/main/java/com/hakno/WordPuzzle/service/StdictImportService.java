package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.client.StdictApiClient;
import com.hakno.WordPuzzle.client.dto.StdictSearchResponse;
import com.hakno.WordPuzzle.client.dto.StdictViewResponse;
import com.hakno.WordPuzzle.dto.ImportProgress;
import com.hakno.WordPuzzle.entity.StdExample;
import com.hakno.WordPuzzle.entity.StdSense;
import com.hakno.WordPuzzle.entity.StdWord;
import com.hakno.WordPuzzle.entity.StdWordRelation;
import com.hakno.WordPuzzle.repository.StdWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class StdictImportService {

    private final StdictApiClient apiClient;
    private final StdWordRepository stdWordRepository;

    private static final int RATE_LIMIT_MS = 100;  // API 호출 간격 (100ms)
    private static final int BATCH_SIZE = 100;     // 한 번에 조회할 개수
    private static final int MAX_RETRIES = 3;      // 재시도 횟수

    private volatile ImportProgress progress = new ImportProgress();
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    /**
     * 임포트 진행 상황 조회
     */
    public ImportProgress getProgress() {
        return progress;
    }

    /**
     * 임포트 중단 요청
     */
    public void stopImport() {
        stopRequested.set(true);
        log.info("Import stop requested");
    }

    // 한글 첫 글자 목록 (가-힣 범위의 초성 시작 글자들)
    private static final String[] KOREAN_FIRST_CHARS = {
        "가", "나", "다", "라", "마", "바", "사", "아", "자", "차", "카", "타", "파", "하"
    };

    /**
     * 특정 글자 수 단어 임포트 (첫 글자별로 검색)
     */
    @Async
    @Transactional
    public void importByLength(int length) {
        log.info("Starting import for {}-character words", length);
        stopRequested.set(false);

        progress = ImportProgress.builder()
                .currentPhase(length + "글자 단어 임포트 중")
                .running(true)
                .startTime(LocalDateTime.now())
                .build();

        try {
            int totalProcessed = 0;

            // 각 첫 글자별로 검색
            for (String firstChar : KOREAN_FIRST_CHARS) {
                if (stopRequested.get()) break;

                progress.setCurrentPhase(length + "글자 '" + firstChar + "'으로 시작하는 단어 임포트 중");
                log.info("Importing {}-char words starting with '{}'", length, firstChar);

                totalProcessed += importByFirstChar(firstChar, length);
            }

            progress.setRunning(false);
            progress.setEndTime(LocalDateTime.now());
            log.info("Completed import for {}-character words. Imported: {}, Skipped: {}, Failed: {}",
                    length, progress.getImported(), progress.getSkipped(), progress.getFailed());

        } catch (Exception e) {
            log.error("Import failed for {}-character words: {}", length, e.getMessage());
            progress.setLastError(e.getMessage());
            progress.setRunning(false);
            progress.setEndTime(LocalDateTime.now());
        }
    }

    /**
     * 특정 첫 글자와 글자 수로 검색하여 임포트
     */
    private int importByFirstChar(String firstChar, int targetLength) {
        int imported = 0;
        int start = 1;

        try {
            // 첫 페이지로 총 개수 파악
            StdictSearchResponse firstResponse = apiClient.searchByFirstChar(firstChar, 1, 10);
            if (firstResponse == null || firstResponse.getChannel() == null) {
                return 0;
            }

            int total = firstResponse.getChannel().getTotal();
            log.debug("Found {} words starting with '{}'", total, firstChar);

            // 페이지별로 처리 (최대 1000까지)
            while (start <= total && start <= 1000 && !stopRequested.get()) {
                StdictSearchResponse response = apiClient.searchByFirstChar(firstChar, start, BATCH_SIZE);
                if (response == null || response.getChannel() == null || response.getChannel().getItem() == null) {
                    break;
                }

                for (StdictSearchResponse.Item item : response.getChannel().getItem()) {
                    if (stopRequested.get()) break;

                    // 글자 수 필터링
                    if (item.getWord() != null && item.getWord().length() == targetLength) {
                        try {
                            importWord(item);
                            imported++;
                        } catch (Exception e) {
                            log.warn("Failed to import word {}: {}", item.getWord(), e.getMessage());
                            progress.setFailed(progress.getFailed() + 1);
                        }
                    }
                    rateLimitDelay();
                }

                start += BATCH_SIZE;
            }
        } catch (Exception e) {
            log.error("Error importing words starting with '{}': {}", firstChar, e.getMessage());
        }

        return imported;
    }

    /**
     * 단일 단어 임포트
     */
    @Transactional
    public void importWord(StdictSearchResponse.Item item) {
        String targetCode = item.getTargetCode();

        // 중복 체크
        if (stdWordRepository.existsByTargetCode(targetCode)) {
            progress.setSkipped(progress.getSkipped() + 1);
            return;
        }

        // 상세 정보 조회
        StdictViewResponse detailResponse = null;
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                detailResponse = apiClient.getWordDetail(targetCode);
                break;
            } catch (Exception e) {
                if (retry == MAX_RETRIES - 1) {
                    throw e;
                }
                exponentialBackoff(retry);
            }
        }

        if (detailResponse == null || detailResponse.getChannel() == null ||
            detailResponse.getChannel().getItem() == null || detailResponse.getChannel().getItem().isEmpty()) {
            // 상세 정보 없으면 검색 결과만으로 저장
            saveBasicWord(item);
            return;
        }

        // 상세 정보로 저장
        saveDetailedWord(item, detailResponse.getChannel().getItem().get(0));
    }

    /**
     * 기본 정보만으로 단어 저장
     */
    private void saveBasicWord(StdictSearchResponse.Item item) {
        Integer supNo = parseSupNo(item.getSupNo());

        StdWord word = StdWord.builder()
                .targetCode(item.getTargetCode())
                .word(item.getWord())
                .supNo(supNo)
                .build();

        // 검색 결과의 의미 정보 저장 (단일 객체)
        if (item.getSense() != null) {
            StdictSearchResponse.Sense s = item.getSense();
            StdSense sense = StdSense.builder()
                    .senseOrder(1)
                    .pos(item.getPos())
                    .definition(s.getDefinition())
                    .category(s.getCat())
                    .type(s.getType())
                    .build();
            word.addSense(sense);
        }

        stdWordRepository.save(word);
        progress.setImported(progress.getImported() + 1);
    }

    /**
     * supNo 문자열을 Integer로 파싱
     */
    private Integer parseSupNo(String supNo) {
        if (supNo == null || supNo.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(supNo);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 상세 정보로 단어 저장
     */
    private void saveDetailedWord(StdictSearchResponse.Item item, StdictViewResponse.Item detail) {
        StdictViewResponse.WordInfo wordInfo = detail.getWordInfo();

        // 어원 정보 추출
        String origin = null;
        if (wordInfo.getOriginalLanguageInfo() != null && !wordInfo.getOriginalLanguageInfo().isEmpty()) {
            origin = wordInfo.getOriginalLanguageInfo().stream()
                    .map(StdictViewResponse.OriginalLanguageInfo::getOriginalLanguage)
                    .filter(s -> s != null && !s.isEmpty())
                    .findFirst()
                    .orElse(null);
        }

        // 발음 정보 추출
        String pronunciation = null;
        if (wordInfo.getPronunciationInfo() != null && !wordInfo.getPronunciationInfo().isEmpty()) {
            pronunciation = wordInfo.getPronunciationInfo().get(0).getPronunciation();
        }

        // supNo 처리: 상세 정보 우선, 없으면 검색 결과 사용
        Integer supNo = wordInfo.getSupNo() != null ? wordInfo.getSupNo() : parseSupNo(item.getSupNo());

        StdWord word = StdWord.builder()
                .targetCode(item.getTargetCode())
                .word(wordInfo.getWord() != null ? wordInfo.getWord() : item.getWord())
                .supNo(supNo)
                .wordType(wordInfo.getWordType())
                .origin(origin)
                .pronunciation(pronunciation)
                .allomorph(wordInfo.getAllomorph())
                .build();

        // 의미 정보 저장
        if (wordInfo.getPosInfo() != null) {
            for (StdictViewResponse.PosInfo posInfo : wordInfo.getPosInfo()) {
                if (posInfo.getCommPatternInfo() == null) continue;

                for (StdictViewResponse.CommPatternInfo patternInfo : posInfo.getCommPatternInfo()) {
                    if (patternInfo.getSenseInfo() == null) continue;

                    for (int i = 0; i < patternInfo.getSenseInfo().size(); i++) {
                        StdictViewResponse.SenseInfo senseInfo = patternInfo.getSenseInfo().get(i);

                        String category = null;
                        if (senseInfo.getCatInfo() != null && !senseInfo.getCatInfo().isEmpty()) {
                            category = senseInfo.getCatInfo().get(0).getCat();
                        }

                        StdSense sense = StdSense.builder()
                                .senseCode(senseInfo.getSenseNo())
                                .senseOrder(i + 1)
                                .pos(posInfo.getPos())
                                .definition(senseInfo.getDefinition())
                                .category(category)
                                .type(senseInfo.getType())
                                .build();

                        // 용례 저장
                        if (senseInfo.getExampleInfo() != null) {
                            for (StdictViewResponse.ExampleInfo exInfo : senseInfo.getExampleInfo()) {
                                StdExample example = StdExample.builder()
                                        .example(exInfo.getExample())
                                        .source(exInfo.getSource())
                                        .translation(exInfo.getTranslation())
                                        .origin(exInfo.getOrigin())
                                        .build();
                                sense.addExample(example);
                            }
                        }

                        // 어휘 관계 저장
                        if (senseInfo.getRelationInfo() != null) {
                            for (StdictViewResponse.RelationInfo relInfo : senseInfo.getRelationInfo()) {
                                StdWordRelation relation = StdWordRelation.builder()
                                        .relationType(relInfo.getType())
                                        .relatedWord(relInfo.getWord())
                                        .relatedTargetCode(relInfo.getLinkTargetCode())
                                        .link(relInfo.getLink())
                                        .build();
                                sense.addRelation(relation);
                            }
                        }

                        word.addSense(sense);
                    }
                }
            }
        }

        stdWordRepository.save(word);
        progress.setImported(progress.getImported() + 1);
    }

    /**
     * Rate limiting delay
     */
    private void rateLimitDelay() {
        try {
            Thread.sleep(RATE_LIMIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Exponential backoff for retries
     */
    private void exponentialBackoff(int attempt) {
        try {
            long delay = (long) Math.pow(2, attempt) * 1000;  // 1s, 2s, 4s...
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 통계 조회
     */
    public ImportStats getStats() {
        long total = stdWordRepository.countAll();
        List<Object[]> byLength = stdWordRepository.countByLength();
        List<Object[]> byType = stdWordRepository.countByWordType();

        return new ImportStats(total, byLength, byType);
    }

    public record ImportStats(long total, List<Object[]> byLength, List<Object[]> byWordType) {}
}

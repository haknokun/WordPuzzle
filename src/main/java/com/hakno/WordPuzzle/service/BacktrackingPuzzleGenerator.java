package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.dto.PuzzleCell;
import com.hakno.WordPuzzle.dto.PuzzleResponse;
import com.hakno.WordPuzzle.dto.PuzzleWord;
import com.hakno.WordPuzzle.entity.StdWord;
import com.hakno.WordPuzzle.repository.StdWordRepository;
import com.hakno.WordPuzzle.util.GridConverter;
import com.hakno.WordPuzzle.util.GridSnapshot;
import com.hakno.WordPuzzle.util.GridUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 백트래킹 기반 퍼즐 생성기
 * 그리디 방식보다 더 높은 품질의 퍼즐을 생성합니다.
 *
 * 주요 특징:
 * - 백트래킹: 막히면 이전 상태로 되돌아가서 다른 경로 시도
 * - 가지치기(Pruning): 불필요한 탐색 조기 종료
 * - 타임아웃: 설정된 시간 내에 최선의 결과 반환
 * - 스코어 기반: PuzzleScorer를 활용하여 품질 평가
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BacktrackingPuzzleGenerator {

    private final StdWordRepository stdWordRepository;
    private final PlacementValidator placementValidator;
    private final GridConverter gridConverter;
    private final PuzzleScorer puzzleScorer;

    // 설정 상수
    private static final long DEFAULT_TIMEOUT_MS = 5000; // 기본 타임아웃 5초
    private static final int MAX_BACKTRACK_DEPTH = 50;   // 최대 백트래킹 깊이
    private static final int WORDS_PER_CANDIDATE = 20;   // 교차점당 검색할 단어 수
    private static final int MAX_CANDIDATES_PER_LEVEL = 10; // 레벨당 최대 후보 수
    private static final int DEFAULT_PARALLEL_SEEDS = 3;  // 기본 병렬 시드 수

    // 병렬 실행용 스레드 풀
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    // 최선의 결과 저장용 (스레드 안전)
    private volatile PuzzleResponse bestResult;
    private volatile double bestScore;
    private volatile boolean timeoutReached;

    /**
     * 백트래킹으로 퍼즐 생성
     */
    public PuzzleResponse generate(int gridSize, int targetWordCount) {
        return generate(gridSize, targetWordCount, null, null, DEFAULT_TIMEOUT_MS);
    }

    /**
     * 다중 시드 병렬 퍼즐 생성 (기본 3개 시드)
     */
    public PuzzleResponse generateParallel(int gridSize, int targetWordCount) {
        return generateParallel(gridSize, targetWordCount, null, null, DEFAULT_TIMEOUT_MS, DEFAULT_PARALLEL_SEEDS);
    }

    /**
     * 다중 시드 병렬 퍼즐 생성
     * 여러 시작 단어로 동시에 퍼즐을 생성하고 최고 점수 결과를 반환합니다.
     *
     * @param gridSize 그리드 크기
     * @param targetWordCount 목표 단어 수
     * @param category 카테고리 필터 (null 가능)
     * @param wordType 단어유형 필터 (null 가능)
     * @param timeoutMs 전체 타임아웃 (밀리초)
     * @param numSeeds 병렬 시드 수
     * @return 최고 점수의 퍼즐
     */
    public PuzzleResponse generateParallel(int gridSize, int targetWordCount,
                                           String category, String wordType,
                                           long timeoutMs, int numSeeds) {
        log.info("병렬 퍼즐 생성 시작: gridSize={}, targetWords={}, seeds={}, timeout={}ms",
                gridSize, targetWordCount, numSeeds, timeoutMs);

        long startTime = System.currentTimeMillis();

        // 시작 단어 후보들 가져오기
        List<StdWord> seedWords = getSeedWords(gridSize, category, wordType, numSeeds);
        if (seedWords.isEmpty()) {
            throw new IllegalStateException("조건에 맞는 시드 단어가 없습니다.");
        }

        // 각 시드에 할당할 시간 계산
        long perSeedTimeout = timeoutMs / numSeeds;

        // 병렬로 퍼즐 생성
        List<CompletableFuture<ScoredPuzzle>> futures = seedWords.stream()
                .map(seedWord -> CompletableFuture.supplyAsync(() ->
                        generateWithSeed(gridSize, targetWordCount, category, wordType,
                                        perSeedTimeout, seedWord), executorService))
                .collect(Collectors.toList());

        // 모든 결과 수집 (타임아웃 적용)
        List<ScoredPuzzle> results = new ArrayList<>();
        for (CompletableFuture<ScoredPuzzle> future : futures) {
            try {
                ScoredPuzzle result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                if (result != null) {
                    results.add(result);
                }
            } catch (TimeoutException e) {
                log.warn("병렬 생성 타임아웃");
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("병렬 생성 실패: {}", e.getMessage());
            }
        }

        // 최고 점수 결과 선택
        ScoredPuzzle best = results.stream()
                .max(Comparator.comparingDouble(sp -> sp.score))
                .orElse(null);

        long elapsed = System.currentTimeMillis() - startTime;

        if (best == null) {
            log.warn("병렬 생성 실패 - Fallback으로 단일 생성 시도");
            return generate(gridSize, targetWordCount, category, wordType, timeoutMs);
        }

        log.info("병렬 생성 완료: {}ms, 시드 {}개 중 {}개 성공, 최고점수={:.1f}, 단어수={}",
                elapsed, numSeeds, results.size(), best.score, best.puzzle.getTotalWords());

        return best.puzzle;
    }

    /**
     * 시드 단어 후보 가져오기
     */
    private List<StdWord> getSeedWords(int gridSize, String category, String wordType, int count) {
        int maxLength = Math.min(gridSize - 2, 6);
        List<StdWord> words;

        if (category != null) {
            words = stdWordRepository.findRandomWordsByCategory(category, 3, maxLength, count * 3);
        } else if (wordType != null) {
            words = stdWordRepository.findRandomWordsByWordType(wordType, 3, maxLength, count * 3);
        } else {
            words = stdWordRepository.findRandomWordsWithSenses(3, maxLength, PageRequest.of(0, count * 3));
        }

        // 공통 글자가 많은 단어 우선 정렬 후 상위 N개 선택
        words.sort((a, b) -> GridUtils.countCommonChars(b.getWord()) - GridUtils.countCommonChars(a.getWord()));

        return words.stream().limit(count).collect(Collectors.toList());
    }

    /**
     * 특정 시드 단어로 퍼즐 생성
     */
    private ScoredPuzzle generateWithSeed(int gridSize, int targetWordCount,
                                          String category, String wordType,
                                          long timeoutMs, StdWord seedWord) {
        try {
            long deadline = System.currentTimeMillis() + timeoutMs;

            char[][] grid = GridUtils.createEmptyGrid(gridSize);
            List<PuzzleWord> placedWords = new ArrayList<>();
            Set<String> usedWords = new HashSet<>();

            // 시드 단어 배치
            int startRow = gridSize / 2;
            int startCol = (gridSize - seedWord.getWord().length()) / 2;

            placeWord(grid, seedWord.getWord(), startRow, startCol, PuzzleWord.Direction.ACROSS);
            placedWords.add(createPuzzleWord(seedWord, startRow, startCol, PuzzleWord.Direction.ACROSS));
            usedWords.add(seedWord.getWord());

            // 스레드 로컬 변수로 백트래킹
            PuzzleResponse[] localBest = {null};
            double[] localBestScore = {-1};

            backtrackWithSeed(grid, placedWords, usedWords, gridSize, targetWordCount,
                            category, wordType, deadline, 0, localBest, localBestScore);

            if (localBest[0] == null) {
                localBest[0] = buildPuzzleResponse(grid, placedWords, gridSize);
                localBestScore[0] = puzzleScorer.calculateScore(localBest[0]);
            }

            log.debug("시드 '{}' 완료: score={:.1f}, words={}",
                    seedWord.getWord(), localBestScore[0], localBest[0].getTotalWords());

            return new ScoredPuzzle(localBest[0], localBestScore[0]);

        } catch (Exception e) {
            log.warn("시드 '{}' 생성 실패: {}", seedWord.getWord(), e.getMessage());
            return null;
        }
    }

    /**
     * 백트래킹 (스레드 안전 버전)
     */
    private void backtrackWithSeed(char[][] grid, List<PuzzleWord> placedWords, Set<String> usedWords,
                                   int gridSize, int targetWordCount,
                                   String category, String wordType,
                                   long deadline, int depth,
                                   PuzzleResponse[] localBest, double[] localBestScore) {

        if (System.currentTimeMillis() >= deadline || depth >= MAX_BACKTRACK_DEPTH) {
            return;
        }

        // 현재 상태 평가
        if (placedWords.size() >= 2) {
            PuzzleResponse current = buildPuzzleResponse(grid, new ArrayList<>(placedWords), gridSize);
            double currentScore = puzzleScorer.calculateScore(current);

            if (currentScore > localBestScore[0] ||
                (currentScore == localBestScore[0] && placedWords.size() > (localBest[0] != null ? localBest[0].getTotalWords() : 0))) {
                localBestScore[0] = currentScore;
                localBest[0] = current;
            }
        }

        // 목표 달성 시 조기 종료
        if (placedWords.size() >= targetWordCount && localBestScore[0] >= 70) {
            return;
        }

        // 교차 후보 및 단어 배치 시도
        List<IntersectionCandidate> candidates = findIntersectionCandidates(grid, gridSize);
        if (candidates.isEmpty()) return;

        Collections.shuffle(candidates);
        int maxCandidates = Math.min(candidates.size(), MAX_CANDIDATES_PER_LEVEL);

        for (int i = 0; i < maxCandidates && System.currentTimeMillis() < deadline; i++) {
            IntersectionCandidate candidate = candidates.get(i);
            List<StdWord> words = findWordsForIntersection(candidate, gridSize, usedWords, category, wordType);

            for (StdWord word : words) {
                if (System.currentTimeMillis() >= deadline) break;

                List<PlacementOption> placements = calculatePlacements(candidate, word);

                for (PlacementOption placement : placements) {
                    if (System.currentTimeMillis() >= deadline) break;

                    if (placementValidator.canPlaceWord(grid, word.getWord(),
                            placement.row, placement.col, placement.direction, gridSize)) {

                        GridSnapshot snapshot = new GridSnapshot(grid, placedWords, usedWords);

                        placeWord(grid, word.getWord(), placement.row, placement.col, placement.direction);
                        placedWords.add(createPuzzleWord(word, placement.row, placement.col, placement.direction));
                        usedWords.add(word.getWord());

                        backtrackWithSeed(grid, placedWords, usedWords, gridSize, targetWordCount,
                                        category, wordType, deadline, depth + 1, localBest, localBestScore);

                        snapshot.restoreTo(grid, placedWords, usedWords);
                    }
                }
            }
        }
    }

    /**
     * 점수가 포함된 퍼즐 결과
     */
    private static class ScoredPuzzle {
        final PuzzleResponse puzzle;
        final double score;

        ScoredPuzzle(PuzzleResponse puzzle, double score) {
            this.puzzle = puzzle;
            this.score = score;
        }
    }

    /**
     * 백트래킹으로 퍼즐 생성 (카테고리/단어유형 필터)
     */
    public PuzzleResponse generate(int gridSize, int targetWordCount,
                                   String category, String wordType, long timeoutMs) {
        log.info("백트래킹 퍼즐 생성 시작: gridSize={}, targetWords={}, timeout={}ms",
                gridSize, targetWordCount, timeoutMs);

        bestResult = null;
        bestScore = -1;
        timeoutReached = false;

        long startTime = System.currentTimeMillis();
        long deadline = startTime + timeoutMs;

        char[][] grid = GridUtils.createEmptyGrid(gridSize);
        List<PuzzleWord> placedWords = new ArrayList<>();
        Set<String> usedWords = new HashSet<>();

        // 첫 번째 단어 배치
        StdWord firstWord = findFirstWord(gridSize, category, wordType);
        if (firstWord == null) {
            throw new IllegalStateException("조건에 맞는 단어가 없습니다.");
        }

        int startRow = gridSize / 2;
        int startCol = (gridSize - firstWord.getWord().length()) / 2;

        placeWord(grid, firstWord.getWord(), startRow, startCol, PuzzleWord.Direction.ACROSS);
        placedWords.add(createPuzzleWord(firstWord, startRow, startCol, PuzzleWord.Direction.ACROSS));
        usedWords.add(firstWord.getWord());

        // 백트래킹 시작
        backtrack(grid, placedWords, usedWords, gridSize, targetWordCount,
                 category, wordType, deadline, 0);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("백트래킹 완료: {}ms, 최고점수={:.1f}, 단어수={}",
                elapsed, bestScore, bestResult != null ? bestResult.getTotalWords() : 0);

        if (bestResult == null) {
            // 백트래킹 실패 시 현재 상태로 결과 생성
            bestResult = buildPuzzleResponse(grid, placedWords, gridSize);
        }

        return bestResult;
    }

    /**
     * 백트래킹 재귀 함수
     */
    private void backtrack(char[][] grid, List<PuzzleWord> placedWords, Set<String> usedWords,
                          int gridSize, int targetWordCount,
                          String category, String wordType,
                          long deadline, int depth) {

        // 타임아웃 체크
        if (System.currentTimeMillis() >= deadline) {
            timeoutReached = true;
            return;
        }

        // 깊이 제한 체크
        if (depth >= MAX_BACKTRACK_DEPTH) {
            return;
        }

        // 현재 상태 평가 및 최선 결과 갱신
        if (placedWords.size() >= 2) {
            PuzzleResponse current = buildPuzzleResponse(grid, placedWords, gridSize);
            double currentScore = puzzleScorer.calculateScore(current);

            if (currentScore > bestScore ||
                (currentScore == bestScore && placedWords.size() > (bestResult != null ? bestResult.getTotalWords() : 0))) {
                bestScore = currentScore;
                bestResult = current;
                log.debug("새로운 최선 결과: score={:.1f}, words={}", currentScore, placedWords.size());
            }
        }

        // 목표 달성 시 조기 종료 (가지치기)
        if (placedWords.size() >= targetWordCount && bestScore >= 70) {
            return;
        }

        // 교차 후보 찾기
        List<IntersectionCandidate> candidates = findIntersectionCandidates(grid, gridSize);
        if (candidates.isEmpty()) {
            return;
        }

        // 후보 수 제한 (가지치기)
        Collections.shuffle(candidates);
        int maxCandidates = Math.min(candidates.size(), MAX_CANDIDATES_PER_LEVEL);

        for (int i = 0; i < maxCandidates && !timeoutReached; i++) {
            IntersectionCandidate candidate = candidates.get(i);

            // 해당 교차점에 맞는 단어들 검색
            List<StdWord> words = findWordsForIntersection(candidate, gridSize, usedWords, category, wordType);

            for (StdWord word : words) {
                if (timeoutReached) break;

                List<PlacementOption> placements = calculatePlacements(candidate, word);

                for (PlacementOption placement : placements) {
                    if (timeoutReached) break;

                    if (placementValidator.canPlaceWord(grid, word.getWord(),
                            placement.row, placement.col, placement.direction, gridSize)) {

                        // 스냅샷 생성 (백트래킹용)
                        GridSnapshot snapshot = new GridSnapshot(grid, placedWords, usedWords);

                        // 단어 배치
                        placeWord(grid, word.getWord(), placement.row, placement.col, placement.direction);
                        PuzzleWord pw = createPuzzleWord(word, placement.row, placement.col, placement.direction);
                        placedWords.add(pw);
                        usedWords.add(word.getWord());

                        // 재귀 호출
                        backtrack(grid, placedWords, usedWords, gridSize, targetWordCount,
                                 category, wordType, deadline, depth + 1);

                        // 백트래킹: 상태 복원
                        snapshot.restoreTo(grid, placedWords, usedWords);
                    }
                }
            }
        }
    }

    /**
     * 첫 번째 단어 찾기
     */
    private StdWord findFirstWord(int gridSize, String category, String wordType) {
        int maxLength = Math.min(gridSize - 2, 6);
        List<StdWord> words;

        if (category != null) {
            words = stdWordRepository.findRandomWordsByCategory(category, 3, maxLength, 50);
        } else if (wordType != null) {
            words = stdWordRepository.findRandomWordsByWordType(wordType, 3, maxLength, 50);
        } else {
            words = stdWordRepository.findRandomWordsWithSenses(3, maxLength, PageRequest.of(0, 50));
        }

        if (words.isEmpty()) return null;

        // 공통 글자를 많이 포함한 단어 우선
        words.sort((a, b) -> GridUtils.countCommonChars(b.getWord()) - GridUtils.countCommonChars(a.getWord()));

        int selectFrom = Math.min(10, words.size());
        return words.get(new Random().nextInt(selectFrom));
    }

    /**
     * 교차 후보 찾기
     */
    private List<IntersectionCandidate> findIntersectionCandidates(char[][] grid, int gridSize) {
        List<IntersectionCandidate> candidates = new ArrayList<>();

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                if (grid[row][col] != '\0') {
                    char existingChar = grid[row][col];

                    // 가로 배치 가능 여부
                    if (isPartOfVerticalWord(grid, row, col) && canExtendHorizontally(grid, row, col, gridSize)) {
                        candidates.add(new IntersectionCandidate(row, col, existingChar, PuzzleWord.Direction.ACROSS));
                    }

                    // 세로 배치 가능 여부
                    if (isPartOfHorizontalWord(grid, row, col) && canExtendVertically(grid, row, col, gridSize)) {
                        candidates.add(new IntersectionCandidate(row, col, existingChar, PuzzleWord.Direction.DOWN));
                    }
                }
            }
        }

        return candidates;
    }

    /**
     * 교차점에 맞는 단어 찾기
     */
    private List<StdWord> findWordsForIntersection(IntersectionCandidate candidate, int gridSize,
                                                    Set<String> usedWords, String category, String wordType) {
        String charStr = String.valueOf(candidate.character);
        List<StdWord> words;

        if (category != null) {
            words = stdWordRepository.findRandomWordsByCategoryContainingChar(
                    category, charStr, 2, gridSize, WORDS_PER_CANDIDATE);
        } else if (wordType != null) {
            words = stdWordRepository.findRandomWordsByWordTypeContainingChar(
                    wordType, charStr, 2, gridSize, WORDS_PER_CANDIDATE);
        } else {
            words = stdWordRepository.findWordsContainingCharWithSenses(
                    charStr, 2, gridSize, PageRequest.of(0, WORDS_PER_CANDIDATE));
        }

        // 이미 사용된 단어 필터링
        List<StdWord> filtered = new ArrayList<>();
        for (StdWord w : words) {
            if (!usedWords.contains(w.getWord()) && w.getWord().indexOf(candidate.character) >= 0) {
                filtered.add(w);
            }
        }

        Collections.shuffle(filtered);
        return filtered;
    }

    /**
     * 배치 위치 계산
     */
    private List<PlacementOption> calculatePlacements(IntersectionCandidate candidate, StdWord word) {
        List<PlacementOption> placements = new ArrayList<>();
        String wordStr = word.getWord();

        for (int i = 0; i < wordStr.length(); i++) {
            if (wordStr.charAt(i) == candidate.character) {
                if (candidate.direction == PuzzleWord.Direction.ACROSS) {
                    placements.add(new PlacementOption(candidate.row, candidate.col - i, candidate.direction));
                } else {
                    placements.add(new PlacementOption(candidate.row - i, candidate.col, candidate.direction));
                }
            }
        }

        return placements;
    }

    // ============== 헬퍼 메서드 ==============

    private boolean isPartOfVerticalWord(char[][] grid, int row, int col) {
        return (row > 0 && grid[row - 1][col] != '\0') ||
               (row < grid.length - 1 && grid[row + 1][col] != '\0');
    }

    private boolean isPartOfHorizontalWord(char[][] grid, int row, int col) {
        return (col > 0 && grid[row][col - 1] != '\0') ||
               (col < grid[0].length - 1 && grid[row][col + 1] != '\0');
    }

    private boolean canExtendHorizontally(char[][] grid, int row, int col, int gridSize) {
        return (col > 0 && grid[row][col - 1] == '\0') ||
               (col < gridSize - 1 && grid[row][col + 1] == '\0');
    }

    private boolean canExtendVertically(char[][] grid, int row, int col, int gridSize) {
        return (row > 0 && grid[row - 1][col] == '\0') ||
               (row < gridSize - 1 && grid[row + 1][col] == '\0');
    }

    private void placeWord(char[][] grid, String word, int startRow, int startCol, PuzzleWord.Direction direction) {
        for (int i = 0; i < word.length(); i++) {
            if (direction == PuzzleWord.Direction.ACROSS) {
                grid[startRow][startCol + i] = word.charAt(i);
            } else {
                grid[startRow + i][startCol] = word.charAt(i);
            }
        }
    }

    private PuzzleWord createPuzzleWord(StdWord word, int startRow, int startCol, PuzzleWord.Direction direction) {
        String definition = "";
        if (word.getSenses() != null && !word.getSenses().isEmpty()) {
            definition = word.getSenses().get(0).getDefinition();
        }
        return PuzzleWord.builder()
                .number(0)
                .word(word.getWord())
                .definition(definition)
                .startRow(startRow)
                .startCol(startCol)
                .direction(direction)
                .build();
    }

    private PuzzleResponse buildPuzzleResponse(char[][] grid, List<PuzzleWord> placedWords, int gridSize) {
        // 중앙 정렬
        centerPuzzle(grid, placedWords, gridSize);

        // 가로/세로 분리 및 정렬
        List<PuzzleWord> acrossWords = new ArrayList<>();
        List<PuzzleWord> downWords = new ArrayList<>();

        for (PuzzleWord pw : placedWords) {
            if (pw.getDirection() == PuzzleWord.Direction.ACROSS) {
                acrossWords.add(pw);
            } else {
                downWords.add(pw);
            }
        }

        acrossWords.sort((a, b) -> a.getStartRow() != b.getStartRow() ?
                a.getStartRow() - b.getStartRow() : a.getStartCol() - b.getStartCol());
        downWords.sort((a, b) -> a.getStartRow() != b.getStartRow() ?
                a.getStartRow() - b.getStartRow() : a.getStartCol() - b.getStartCol());

        // 번호 부여
        List<PuzzleWord> numberedAcross = new ArrayList<>();
        int num = 1;
        for (PuzzleWord pw : acrossWords) {
            numberedAcross.add(PuzzleWord.builder()
                    .number(num++).word(pw.getWord()).definition(pw.getDefinition())
                    .startRow(pw.getStartRow()).startCol(pw.getStartCol()).direction(pw.getDirection()).build());
        }

        List<PuzzleWord> numberedDown = new ArrayList<>();
        num = 1;
        for (PuzzleWord pw : downWords) {
            numberedDown.add(PuzzleWord.builder()
                    .number(num++).word(pw.getWord()).definition(pw.getDefinition())
                    .startRow(pw.getStartRow()).startCol(pw.getStartCol()).direction(pw.getDirection()).build());
        }

        List<List<PuzzleCell>> cellGrid = gridConverter.convertToCellGrid(grid, numberedAcross, numberedDown, gridSize);

        return PuzzleResponse.builder()
                .gridSize(gridSize)
                .grid(cellGrid)
                .acrossWords(numberedAcross)
                .downWords(numberedDown)
                .totalWords(placedWords.size())
                .build();
    }

    private void centerPuzzle(char[][] grid, List<PuzzleWord> placedWords, int gridSize) {
        int minRow = gridSize, maxRow = 0, minCol = gridSize, maxCol = 0;
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                if (grid[row][col] != '\0') {
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        if (minRow > maxRow) return;

        int puzzleHeight = maxRow - minRow + 1;
        int puzzleWidth = maxCol - minCol + 1;
        int targetMinRow = (gridSize - puzzleHeight) / 2;
        int targetMinCol = (gridSize - puzzleWidth) / 2;
        int rowOffset = targetMinRow - minRow;
        int colOffset = targetMinCol - minCol;

        if (rowOffset == 0 && colOffset == 0) return;

        char[][] newGrid = GridUtils.createEmptyGrid(gridSize);
        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                if (grid[row][col] != '\0') {
                    newGrid[row + rowOffset][col + colOffset] = grid[row][col];
                }
            }
        }

        for (int row = 0; row < gridSize; row++) {
            System.arraycopy(newGrid[row], 0, grid[row], 0, gridSize);
        }

        for (PuzzleWord pw : placedWords) {
            pw.setStartRow(pw.getStartRow() + rowOffset);
            pw.setStartCol(pw.getStartCol() + colOffset);
        }
    }

    // ============== 내부 클래스 ==============

    private static class IntersectionCandidate {
        int row, col;
        char character;
        PuzzleWord.Direction direction;

        IntersectionCandidate(int row, int col, char character, PuzzleWord.Direction direction) {
            this.row = row;
            this.col = col;
            this.character = character;
            this.direction = direction;
        }
    }

    private static class PlacementOption {
        int row, col;
        PuzzleWord.Direction direction;

        PlacementOption(int row, int col, PuzzleWord.Direction direction) {
            this.row = row;
            this.col = col;
            this.direction = direction;
        }
    }
}

package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.dto.PuzzleCell;
import com.hakno.WordPuzzle.dto.PuzzleResponse;
import com.hakno.WordPuzzle.dto.PuzzleWord;
import com.hakno.WordPuzzle.entity.StdWord;
import com.hakno.WordPuzzle.entity.Word;
import com.hakno.WordPuzzle.repository.StdWordRepository;
import com.hakno.WordPuzzle.repository.WordRepository;
import com.hakno.WordPuzzle.util.GridConverter;
import com.hakno.WordPuzzle.util.GridUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PuzzleGeneratorService {

    private final WordRepository wordRepository;
    private final StdWordRepository stdWordRepository;
    private final PlacementValidator placementValidator;
    private final GridConverter gridConverter;

    private static final int MAX_ATTEMPTS = 200;
    private static final int SEARCH_LIMIT = 100;

    // 데이터 소스 상수
    public static final String SOURCE_DEFAULT = "default";
    public static final String SOURCE_STD = "std";

    public PuzzleResponse generatePuzzle(Integer gridSize, int targetWordCount) {
        return generatePuzzle(gridSize, targetWordCount, null, SOURCE_DEFAULT);
    }

    public PuzzleResponse generatePuzzle(Integer gridSize, int targetWordCount, String level) {
        return generatePuzzle(gridSize, targetWordCount, level, SOURCE_DEFAULT);
    }

    public PuzzleResponse generatePuzzle(Integer gridSize, int targetWordCount, String level, String source) {
        if (SOURCE_STD.equalsIgnoreCase(source)) {
            return generatePuzzleFromStd(gridSize, targetWordCount);
        }
        return generatePuzzleFromDefault(gridSize, targetWordCount, level);
    }

    private PuzzleResponse generatePuzzleFromDefault(Integer gridSize, int targetWordCount, String level) {
        // gridSize가 null이면 단어 수에 따라 자동 계산
        int actualGridSize = (gridSize != null) ? gridSize : GridUtils.calculateGridSize(targetWordCount);

        // 최대 3번 재시도
        for (int retry = 0; retry < 3; retry++) {
            PuzzleResponse result = tryGeneratePuzzle(actualGridSize, targetWordCount, level);
            if (result.getTotalWords() >= targetWordCount * 0.7) { // 70% 이상 달성하면 성공
                return result;
            }
            log.info("퍼즐 생성 재시도 {}/3 - 목표: {}, 달성: {}", retry + 1, targetWordCount, result.getTotalWords());
        }
        // 3번 시도 후에도 실패하면 마지막 결과 반환
        return tryGeneratePuzzle(actualGridSize, targetWordCount, level);
    }

    private PuzzleResponse tryGeneratePuzzle(int gridSize, int targetWordCount, String level) {
        char[][] grid = GridUtils.createEmptyGrid(gridSize);

        List<PuzzleWord> placedWords = new ArrayList<>();
        Set<String> usedWords = new HashSet<>();

        // 첫 번째 단어 배치 (중앙에 가로로) - 공통 글자가 많은 단어 선호
        Word firstWord = findFirstWord(gridSize, level);
        if (firstWord == null) {
            throw new IllegalStateException("단어 데이터가 없습니다. 먼저 데이터를 import 해주세요.");
        }

        int startRow = gridSize / 2;
        int startCol = (gridSize - firstWord.getLength()) / 2;

        placeWord(grid, firstWord.getWord(), startRow, startCol, PuzzleWord.Direction.ACROSS);
        placedWords.add(createPuzzleWord(firstWord, 0, startRow, startCol, PuzzleWord.Direction.ACROSS));
        usedWords.add(firstWord.getWord());

        // 나머지 단어들 배치 시도 (On-Demand 방식)
        int attempts = 0;
        int totalAttempts = 0;
        while (placedWords.size() < targetWordCount && attempts < MAX_ATTEMPTS) {
            attempts++;
            totalAttempts++;

            // 그리드에서 교차 가능한 위치들 수집
            List<IntersectionCandidate> candidates = findIntersectionCandidates(grid, gridSize);
            if (candidates.isEmpty()) {
                log.info("교차 가능한 위치가 없음. 현재 단어 수: {}", placedWords.size());
                break;
            }

            Collections.shuffle(candidates);
            boolean placed = false;

            int totalWordsChecked = 0;
            int totalPlacementsChecked = 0;
            Map<String, Integer> failureReasons = new HashMap<>();

            candidateLoop:
            for (IntersectionCandidate candidate : candidates) {
                // 해당 교차점에 맞는 단어들을 DB에서 검색
                List<Word> words = findWordsForIntersection(candidate, gridSize, usedWords, level);
                totalWordsChecked += words.size();

                for (Word word : words) {
                    // 단어에서 해당 글자가 나타나는 모든 위치 시도
                    List<PlacementResult> placements = calculateAllPlacements(candidate, word);
                    totalPlacementsChecked += placements.size();

                    for (PlacementResult placement : placements) {
                        if (placementValidator.canPlaceWord(grid, word.getWord(), placement.row, placement.col, placement.direction, gridSize)) {
                            placeWord(grid, word.getWord(), placement.row, placement.col, placement.direction);
                            placedWords.add(createPuzzleWord(word, 0, placement.row, placement.col, placement.direction));
                            usedWords.add(word.getWord());
                            placed = true;
                            attempts = 0;
                            log.debug("단어 배치 성공: {} (총 {}개)", word.getWord(), placedWords.size());
                            break candidateLoop;
                        } else {
                            String reason = placementValidator.whyCannotPlace(grid, word.getWord(), placement.row, placement.col, placement.direction, gridSize);
                            failureReasons.merge(reason, 1, Integer::sum);
                        }
                    }
                }
            }

            if (!placed) {
                log.info("배치 실패 - 후보위치: {}, 검사단어: {}, 배치시도: {}, 실패이유: {}",
                    candidates.size(), totalWordsChecked, totalPlacementsChecked, failureReasons);
                attempts++;
            }
        }

        log.info("퍼즐 생성 완료: 목표 {}개, 실제 {}개, 총 시도 {}회", targetWordCount, placedWords.size(), totalAttempts);

        // 퍼즐 중앙 정렬
        centerPuzzle(grid, placedWords, gridSize);

        // 가로/세로 단어 분리 및 번호 부여
        List<PuzzleWord> acrossWords = new ArrayList<>();
        List<PuzzleWord> downWords = new ArrayList<>();

        for (PuzzleWord pw : placedWords) {
            if (pw.getDirection() == PuzzleWord.Direction.ACROSS) {
                acrossWords.add(pw);
            } else {
                downWords.add(pw);
            }
        }

        // 가로 단어 정렬 후 번호 부여
        acrossWords.sort((a, b) -> {
            if (a.getStartRow() != b.getStartRow()) {
                return a.getStartRow() - b.getStartRow();
            }
            return a.getStartCol() - b.getStartCol();
        });

        List<PuzzleWord> numberedAcrossWords = new ArrayList<>();
        int acrossNum = 1;
        for (PuzzleWord pw : acrossWords) {
            numberedAcrossWords.add(PuzzleWord.builder()
                    .number(acrossNum++)
                    .word(pw.getWord())
                    .definition(pw.getDefinition())
                    .startRow(pw.getStartRow())
                    .startCol(pw.getStartCol())
                    .direction(pw.getDirection())
                    .build());
        }

        // 세로 단어 정렬 후 번호 부여
        downWords.sort((a, b) -> {
            if (a.getStartRow() != b.getStartRow()) {
                return a.getStartRow() - b.getStartRow();
            }
            return a.getStartCol() - b.getStartCol();
        });

        List<PuzzleWord> numberedDownWords = new ArrayList<>();
        int downNum = 1;
        for (PuzzleWord pw : downWords) {
            numberedDownWords.add(PuzzleWord.builder()
                    .number(downNum++)
                    .word(pw.getWord())
                    .definition(pw.getDefinition())
                    .startRow(pw.getStartRow())
                    .startCol(pw.getStartCol())
                    .direction(pw.getDirection())
                    .build());
        }

        // 그리드를 PuzzleCell로 변환
        List<List<PuzzleCell>> cellGrid = gridConverter.convertToCellGrid(grid, numberedAcrossWords, numberedDownWords, gridSize);

        return PuzzleResponse.builder()
                .gridSize(gridSize)
                .grid(cellGrid)
                .acrossWords(numberedAcrossWords)
                .downWords(numberedDownWords)
                .totalWords(placedWords.size())
                .build();
    }

    private Word findFirstWord(int gridSize, String level) {
        int maxLength = Math.min(gridSize - 2, 6);
        List<Word> words = wordRepository.findRandomWordsWithDefinitionsByLevel(
            3, maxLength, level, PageRequest.of(0, 50)
        );
        if (words.isEmpty()) {
            return null;
        }

        // 공통 글자를 많이 포함한 단어 우선 선택
        words.sort((a, b) -> {
            int scoreA = GridUtils.countCommonChars(a.getWord());
            int scoreB = GridUtils.countCommonChars(b.getWord());
            return scoreB - scoreA; // 내림차순
        });

        // 상위 10개 중 랜덤 선택 (다양성 유지)
        int selectFrom = Math.min(10, words.size());
        return words.get(new Random().nextInt(selectFrom));
    }

    private List<IntersectionCandidate> findIntersectionCandidates(char[][] grid, int gridSize) {
        List<IntersectionCandidate> candidates = new ArrayList<>();

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                if (grid[row][col] != '\0') {
                    char existingChar = grid[row][col];

                    // 가로 배치 가능 여부 체크 (현재 셀이 세로 단어의 일부인 경우)
                    if (isPartOfVerticalWord(grid, row, col) && canExtendHorizontally(grid, row, col, gridSize)) {
                        candidates.add(new IntersectionCandidate(row, col, existingChar, PuzzleWord.Direction.ACROSS));
                    }

                    // 세로 배치 가능 여부 체크 (현재 셀이 가로 단어의 일부인 경우)
                    if (isPartOfHorizontalWord(grid, row, col) && canExtendVertically(grid, row, col, gridSize)) {
                        candidates.add(new IntersectionCandidate(row, col, existingChar, PuzzleWord.Direction.DOWN));
                    }
                }
            }
        }

        return candidates;
    }

    private boolean canExtendHorizontally(char[][] grid, int row, int col, int gridSize) {
        // 좌우에 빈 공간이 있어야 함
        boolean hasLeftSpace = col > 0 && grid[row][col - 1] == '\0';
        boolean hasRightSpace = col < gridSize - 1 && grid[row][col + 1] == '\0';
        return hasLeftSpace || hasRightSpace;
    }

    private boolean canExtendVertically(char[][] grid, int row, int col, int gridSize) {
        // 상하에 빈 공간이 있어야 함
        boolean hasTopSpace = row > 0 && grid[row - 1][col] == '\0';
        boolean hasBottomSpace = row < gridSize - 1 && grid[row + 1][col] == '\0';
        return hasTopSpace || hasBottomSpace;
    }

    @Cacheable(value = "wordsContaining", key = "#candidate.character + '_' + #gridSize + '_' + #level")
    private List<Word> getCachedWordsContaining(IntersectionCandidate candidate, int gridSize, String level) {
        return wordRepository.findByContainingCharacterWithDefinitionsByLevel(
            String.valueOf(candidate.character),
            2, gridSize,
            level,
            PageRequest.of(0, SEARCH_LIMIT)
        );
    }

    private List<Word> findWordsForIntersection(IntersectionCandidate candidate, int gridSize, Set<String> usedWords, String level) {
        List<Word> words = getCachedWordsContaining(candidate, gridSize, level);

        List<Word> filtered = words.stream()
            .filter(w -> !usedWords.contains(w.getWord()))
            .filter(w -> w.getWord().indexOf(candidate.character) >= 0)
            .collect(java.util.stream.Collectors.toList());

        // 다양성을 위해 셔플
        Collections.shuffle(filtered);
        return filtered;
    }

    private List<PlacementResult> calculateAllPlacements(IntersectionCandidate candidate, Word word) {
        List<PlacementResult> placements = new ArrayList<>();
        String wordStr = word.getWord();

        // 단어에서 해당 글자가 나타나는 모든 위치 찾기
        for (int i = 0; i < wordStr.length(); i++) {
            if (wordStr.charAt(i) == candidate.character) {
                if (candidate.direction == PuzzleWord.Direction.ACROSS) {
                    int startCol = candidate.col - i;
                    placements.add(new PlacementResult(candidate.row, startCol, candidate.direction, 1));
                } else {
                    int startRow = candidate.row - i;
                    placements.add(new PlacementResult(startRow, candidate.col, candidate.direction, 1));
                }
            }
        }

        return placements;
    }

    private boolean isPartOfVerticalWord(char[][] grid, int row, int col) {
        return (row > 0 && grid[row - 1][col] != '\0') || (row < grid.length - 1 && grid[row + 1][col] != '\0');
    }

    private boolean isPartOfHorizontalWord(char[][] grid, int row, int col) {
        return (col > 0 && grid[row][col - 1] != '\0') || (col < grid[0].length - 1 && grid[row][col + 1] != '\0');
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

    /**
     * 퍼즐을 그리드 중앙으로 정렬
     */
    private void centerPuzzle(char[][] grid, List<PuzzleWord> placedWords, int gridSize) {
        // 1. 사용된 셀들의 bounding box 계산
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

        if (minRow > maxRow) return; // 빈 그리드

        // 2. 이동할 offset 계산
        int puzzleHeight = maxRow - minRow + 1;
        int puzzleWidth = maxCol - minCol + 1;
        int targetMinRow = (gridSize - puzzleHeight) / 2;
        int targetMinCol = (gridSize - puzzleWidth) / 2;
        int rowOffset = targetMinRow - minRow;
        int colOffset = targetMinCol - minCol;

        if (rowOffset == 0 && colOffset == 0) return; // 이미 중앙

        // 3. 새 그리드에 복사
        char[][] newGrid = GridUtils.createEmptyGrid(gridSize);
        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                if (grid[row][col] != '\0') {
                    newGrid[row + rowOffset][col + colOffset] = grid[row][col];
                }
            }
        }

        // 4. 원본 그리드에 복사
        for (int row = 0; row < gridSize; row++) {
            System.arraycopy(newGrid[row], 0, grid[row], 0, gridSize);
        }

        // 5. 단어 위치 조정
        for (PuzzleWord pw : placedWords) {
            pw.setStartRow(pw.getStartRow() + rowOffset);
            pw.setStartCol(pw.getStartCol() + colOffset);
        }

        log.debug("퍼즐 중앙 정렬: rowOffset={}, colOffset={}", rowOffset, colOffset);
    }

    private PuzzleWord createPuzzleWord(Word word, int number, int startRow, int startCol, PuzzleWord.Direction direction) {
        String definition = word.getDefinitions().isEmpty() ? "" : word.getDefinitions().get(0).getDefinition();
        return PuzzleWord.builder()
                .number(number)
                .word(word.getWord())
                .definition(definition)
                .startRow(startRow)
                .startCol(startCol)
                .direction(direction)
                .build();
    }

    // ==================== StdWord 기반 퍼즐 생성 ====================

    private PuzzleResponse generatePuzzleFromStd(Integer gridSize, int targetWordCount) {
        int actualGridSize = (gridSize != null) ? gridSize : GridUtils.calculateGridSize(targetWordCount);

        for (int retry = 0; retry < 3; retry++) {
            PuzzleResponse result = tryGeneratePuzzleFromStd(actualGridSize, targetWordCount);
            if (result.getTotalWords() >= targetWordCount * 0.7) {
                return result;
            }
            log.info("StdWord 퍼즐 생성 재시도 {}/3 - 목표: {}, 달성: {}", retry + 1, targetWordCount, result.getTotalWords());
        }
        return tryGeneratePuzzleFromStd(actualGridSize, targetWordCount);
    }

    private PuzzleResponse tryGeneratePuzzleFromStd(int gridSize, int targetWordCount) {
        char[][] grid = GridUtils.createEmptyGrid(gridSize);
        List<PuzzleWord> placedWords = new ArrayList<>();
        Set<String> usedWords = new HashSet<>();

        // 첫 번째 단어 배치
        StdWord firstWord = findFirstStdWord(gridSize);
        if (firstWord == null) {
            throw new IllegalStateException("StdWord 데이터가 없습니다. 먼저 데이터를 import 해주세요.");
        }

        int startRow = gridSize / 2;
        int startCol = (gridSize - firstWord.getWord().length()) / 2;

        placeWord(grid, firstWord.getWord(), startRow, startCol, PuzzleWord.Direction.ACROSS);
        placedWords.add(createPuzzleWordFromStd(firstWord, 0, startRow, startCol, PuzzleWord.Direction.ACROSS));
        usedWords.add(firstWord.getWord());

        // 나머지 단어 배치
        int attempts = 0;
        while (placedWords.size() < targetWordCount && attempts < MAX_ATTEMPTS) {
            attempts++;

            List<IntersectionCandidate> candidates = findIntersectionCandidates(grid, gridSize);
            if (candidates.isEmpty()) break;

            Collections.shuffle(candidates);
            boolean placed = false;

            candidateLoop:
            for (IntersectionCandidate candidate : candidates) {
                List<StdWord> words = findStdWordsForIntersection(candidate, gridSize, usedWords);

                for (StdWord word : words) {
                    List<PlacementResult> placements = calculateAllPlacementsForStd(candidate, word);

                    for (PlacementResult placement : placements) {
                        if (placementValidator.canPlaceWord(grid, word.getWord(), placement.row, placement.col, placement.direction, gridSize)) {
                            placeWord(grid, word.getWord(), placement.row, placement.col, placement.direction);
                            placedWords.add(createPuzzleWordFromStd(word, 0, placement.row, placement.col, placement.direction));
                            usedWords.add(word.getWord());
                            placed = true;
                            attempts = 0;
                            break candidateLoop;
                        }
                    }
                }
            }

            if (!placed) attempts++;
        }

        log.info("StdWord 퍼즐 생성 완료: 목표 {}개, 실제 {}개", targetWordCount, placedWords.size());

        // 퍼즐 중앙 정렬
        centerPuzzle(grid, placedWords, gridSize);

        // 가로/세로 분리 및 번호 부여
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
                .gridSize(gridSize).grid(cellGrid)
                .acrossWords(numberedAcross).downWords(numberedDown)
                .totalWords(placedWords.size()).build();
    }

    private StdWord findFirstStdWord(int gridSize) {
        int maxLength = Math.min(gridSize - 2, 6);
        List<StdWord> words = stdWordRepository.findRandomWordsWithSenses(3, maxLength, PageRequest.of(0, 50));
        if (words.isEmpty()) return null;

        words.sort((a, b) -> GridUtils.countCommonChars(b.getWord()) - GridUtils.countCommonChars(a.getWord()));
        int selectFrom = Math.min(10, words.size());
        return words.get(new Random().nextInt(selectFrom));
    }

    private List<StdWord> findStdWordsForIntersection(IntersectionCandidate candidate, int gridSize, Set<String> usedWords) {
        List<StdWord> words = stdWordRepository.findWordsContainingCharWithSenses(
                String.valueOf(candidate.character), 2, gridSize, PageRequest.of(0, SEARCH_LIMIT));

        List<StdWord> filtered = words.stream()
                .filter(w -> !usedWords.contains(w.getWord()))
                .filter(w -> w.getWord().indexOf(candidate.character) >= 0)
                .collect(java.util.stream.Collectors.toList());

        Collections.shuffle(filtered);
        return filtered;
    }

    private List<PlacementResult> calculateAllPlacementsForStd(IntersectionCandidate candidate, StdWord word) {
        List<PlacementResult> placements = new ArrayList<>();
        String wordStr = word.getWord();

        for (int i = 0; i < wordStr.length(); i++) {
            if (wordStr.charAt(i) == candidate.character) {
                if (candidate.direction == PuzzleWord.Direction.ACROSS) {
                    placements.add(new PlacementResult(candidate.row, candidate.col - i, candidate.direction, 1));
                } else {
                    placements.add(new PlacementResult(candidate.row - i, candidate.col, candidate.direction, 1));
                }
            }
        }
        return placements;
    }

    private PuzzleWord createPuzzleWordFromStd(StdWord word, int number, int startRow, int startCol, PuzzleWord.Direction direction) {
        String definition = "";
        if (word.getSenses() != null && !word.getSenses().isEmpty()) {
            definition = word.getSenses().get(0).getDefinition();
        }
        return PuzzleWord.builder()
                .number(number).word(word.getWord()).definition(definition)
                .startRow(startRow).startCol(startCol).direction(direction).build();
    }

    // ==================== 공통 클래스 ====================

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

    private static class PlacementResult {
        int row, col, intersections;
        PuzzleWord.Direction direction;

        PlacementResult(int row, int col, PuzzleWord.Direction direction, int intersections) {
            this.row = row;
            this.col = col;
            this.direction = direction;
            this.intersections = intersections;
        }
    }
}

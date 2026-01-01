package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.dto.PuzzleCell;
import com.hakno.WordPuzzle.dto.PuzzleResponse;
import com.hakno.WordPuzzle.dto.PuzzleWord;
import com.hakno.WordPuzzle.entity.Word;
import com.hakno.WordPuzzle.repository.WordRepository;
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
    private static final int MAX_ATTEMPTS = 200;
    private static final int SEARCH_LIMIT = 100;

    public PuzzleResponse generatePuzzle(Integer gridSize, int targetWordCount) {
        return generatePuzzle(gridSize, targetWordCount, null);
    }

    public PuzzleResponse generatePuzzle(Integer gridSize, int targetWordCount, String level) {
        // gridSize가 null이면 단어 수에 따라 자동 계산
        int actualGridSize = (gridSize != null) ? gridSize : calculateGridSize(targetWordCount);

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

    /**
     * 단어 수에 따라 적절한 그리드 크기를 계산
     * - 단어가 많을수록 더 큰 그리드 필요
     * - 최소 10, 최대 25
     */
    private int calculateGridSize(int wordCount) {
        // 기본 공식: 8 + (단어수 * 0.7)
        int size = (int) Math.round(8 + wordCount * 0.7);
        return Math.max(10, Math.min(25, size));
    }

    private PuzzleResponse tryGeneratePuzzle(int gridSize, int targetWordCount, String level) {
        char[][] grid = new char[gridSize][gridSize];
        for (char[] row : grid) {
            Arrays.fill(row, '\0');
        }

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
                        if (canPlaceWord(grid, word.getWord(), placement.row, placement.col, placement.direction, gridSize)) {
                            placeWord(grid, word.getWord(), placement.row, placement.col, placement.direction);
                            placedWords.add(createPuzzleWord(word, 0, placement.row, placement.col, placement.direction));
                            usedWords.add(word.getWord());
                            placed = true;
                            attempts = 0;
                            log.debug("단어 배치 성공: {} (총 {}개)", word.getWord(), placedWords.size());
                            break candidateLoop;
                        } else {
                            String reason = whyCannotPlace(grid, word.getWord(), placement.row, placement.col, placement.direction, gridSize);
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
        List<List<PuzzleCell>> cellGrid = convertToCellGrid(grid, numberedAcrossWords, numberedDownWords, gridSize);

        return PuzzleResponse.builder()
                .gridSize(gridSize)
                .grid(cellGrid)
                .acrossWords(numberedAcrossWords)
                .downWords(numberedDownWords)
                .totalWords(placedWords.size())
                .build();
    }

    // 자주 쓰이는 한글 글자들 (교차 가능성 높음)
    private static final String COMMON_CHARS = "가나다라마바사아자하이의를을에서는로고기대";

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
            int scoreA = countCommonChars(a.getWord());
            int scoreB = countCommonChars(b.getWord());
            return scoreB - scoreA; // 내림차순
        });

        // 상위 10개 중 랜덤 선택 (다양성 유지)
        int selectFrom = Math.min(10, words.size());
        return words.get(new Random().nextInt(selectFrom));
    }

    private int countCommonChars(String word) {
        int count = 0;
        for (char c : word.toCharArray()) {
            if (COMMON_CHARS.indexOf(c) >= 0) {
                count++;
            }
        }
        return count;
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

    private boolean canPlaceWord(char[][] grid, String word, int startRow, int startCol, PuzzleWord.Direction direction, int gridSize) {
        int len = word.length();

        if (direction == PuzzleWord.Direction.ACROSS) {
            // 범위 체크
            if (startCol < 0 || startCol + len > gridSize) return false;
            // 단어 앞뒤에 빈 칸 확보
            if (startCol > 0 && grid[startRow][startCol - 1] != '\0') return false;
            if (startCol + len < gridSize && grid[startRow][startCol + len] != '\0') return false;

            boolean hasIntersection = false;
            for (int i = 0; i < len; i++) {
                int col = startCol + i;
                char existing = grid[startRow][col];
                char newChar = word.charAt(i);

                if (existing != '\0') {
                    // 교차점: 같은 글자여야 함
                    if (existing != newChar) return false;
                    hasIntersection = true;
                } else {
                    // 빈 셀: 교차점이 아니면 위/아래에 글자가 있으면 안됨 (단어 분리)
                    boolean hasAbove = startRow > 0 && grid[startRow - 1][col] != '\0';
                    boolean hasBelow = startRow < gridSize - 1 && grid[startRow + 1][col] != '\0';

                    if (hasAbove || hasBelow) {
                        return false;
                    }
                }
            }
            return hasIntersection;

        } else {
            // 범위 체크
            if (startRow < 0 || startRow + len > gridSize) return false;
            // 단어 앞뒤에 빈 칸 확보
            if (startRow > 0 && grid[startRow - 1][startCol] != '\0') return false;
            if (startRow + len < gridSize && grid[startRow + len][startCol] != '\0') return false;

            boolean hasIntersection = false;
            for (int i = 0; i < len; i++) {
                int row = startRow + i;
                char existing = grid[row][startCol];
                char newChar = word.charAt(i);

                if (existing != '\0') {
                    // 교차점: 같은 글자여야 함
                    if (existing != newChar) return false;
                    hasIntersection = true;
                } else {
                    // 빈 셀: 교차점이 아니면 좌/우에 글자가 있으면 안됨 (단어 분리)
                    boolean hasLeft = startCol > 0 && grid[row][startCol - 1] != '\0';
                    boolean hasRight = startCol < gridSize - 1 && grid[row][startCol + 1] != '\0';

                    if (hasLeft || hasRight) {
                        return false;
                    }
                }
            }
            return hasIntersection;
        }
    }

    // 디버깅용: 왜 배치가 안되는지 상세 이유 반환
    private String whyCannotPlace(char[][] grid, String word, int startRow, int startCol, PuzzleWord.Direction direction, int gridSize) {
        int len = word.length();

        if (direction == PuzzleWord.Direction.ACROSS) {
            if (startCol < 0) return "시작열 음수";
            if (startCol + len > gridSize) return "범위초과(열)";
            if (startCol > 0 && grid[startRow][startCol - 1] != '\0') return "앞에 글자있음";
            if (startCol + len < gridSize && grid[startRow][startCol + len] != '\0') return "뒤에 글자있음";

            boolean hasIntersection = false;
            for (int i = 0; i < len; i++) {
                int col = startCol + i;
                char existing = grid[startRow][col];
                char newChar = word.charAt(i);

                if (existing != '\0') {
                    if (existing != newChar) return "교차점 글자불일치: " + existing + "!=" + newChar;
                    hasIntersection = true;
                } else {
                    boolean hasAbove = startRow > 0 && grid[startRow - 1][col] != '\0';
                    boolean hasBelow = startRow < gridSize - 1 && grid[startRow + 1][col] != '\0';
                    if (hasAbove) return "위에 인접글자";
                    if (hasBelow) return "아래에 인접글자";
                }
            }
            if (!hasIntersection) return "교차점 없음";
        } else {
            if (startRow < 0) return "시작행 음수";
            if (startRow + len > gridSize) return "범위초과(행)";
            if (startRow > 0 && grid[startRow - 1][startCol] != '\0') return "앞에 글자있음";
            if (startRow + len < gridSize && grid[startRow + len][startCol] != '\0') return "뒤에 글자있음";

            boolean hasIntersection = false;
            for (int i = 0; i < len; i++) {
                int row = startRow + i;
                char existing = grid[row][startCol];
                char newChar = word.charAt(i);

                if (existing != '\0') {
                    if (existing != newChar) return "교차점 글자불일치: " + existing + "!=" + newChar;
                    hasIntersection = true;
                } else {
                    boolean hasLeft = startCol > 0 && grid[row][startCol - 1] != '\0';
                    boolean hasRight = startCol < gridSize - 1 && grid[row][startCol + 1] != '\0';
                    if (hasLeft) return "좌측에 인접글자";
                    if (hasRight) return "우측에 인접글자";
                }
            }
            if (!hasIntersection) return "교차점 없음";
        }
        return "OK";
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

    private List<List<PuzzleCell>> convertToCellGrid(char[][] grid, List<PuzzleWord> acrossWords, List<PuzzleWord> downWords, int gridSize) {
        Map<String, Integer> acrossNumbers = new HashMap<>();
        for (PuzzleWord word : acrossWords) {
            String key = word.getStartRow() + "," + word.getStartCol();
            acrossNumbers.put(key, word.getNumber());
        }

        Map<String, Integer> downNumbers = new HashMap<>();
        for (PuzzleWord word : downWords) {
            String key = word.getStartRow() + "," + word.getStartCol();
            downNumbers.put(key, word.getNumber());
        }

        List<List<PuzzleCell>> cellGrid = new ArrayList<>();
        for (int row = 0; row < gridSize; row++) {
            List<PuzzleCell> rowCells = new ArrayList<>();
            for (int col = 0; col < gridSize; col++) {
                String key = row + "," + col;
                char c = grid[row][col];
                rowCells.add(PuzzleCell.builder()
                        .row(row)
                        .col(col)
                        .letter(c == '\0' ? null : String.valueOf(c))
                        .isBlank(c == '\0')
                        .acrossNumber(acrossNumbers.get(key))
                        .downNumber(downNumbers.get(key))
                        .build());
            }
            cellGrid.add(rowCells);
        }
        return cellGrid;
    }

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

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
    private static final int MAX_ATTEMPTS = 100;
    private static final int SEARCH_LIMIT = 50;

    public PuzzleResponse generatePuzzle(int gridSize, int targetWordCount) {
        char[][] grid = new char[gridSize][gridSize];
        for (char[] row : grid) {
            Arrays.fill(row, '\0');
        }

        List<PuzzleWord> placedWords = new ArrayList<>();
        Set<String> usedWords = new HashSet<>();

        // 첫 번째 단어 배치 (중앙에 가로로)
        Word firstWord = findFirstWord(gridSize);
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
        while (placedWords.size() < targetWordCount && attempts < MAX_ATTEMPTS) {
            attempts++;

            // 그리드에서 교차 가능한 위치들 수집
            List<IntersectionCandidate> candidates = findIntersectionCandidates(grid, gridSize);
            if (candidates.isEmpty()) {
                break;
            }

            Collections.shuffle(candidates);
            boolean placed = false;

            for (IntersectionCandidate candidate : candidates) {
                // 해당 교차점에 맞는 단어를 DB에서 검색
                Word word = findWordForIntersection(candidate, gridSize, usedWords);

                if (word != null) {
                    PlacementResult placement = calculatePlacement(candidate, word);
                    if (placement != null && canPlaceWord(grid, word.getWord(), placement.row, placement.col, placement.direction, gridSize)) {
                        placeWord(grid, word.getWord(), placement.row, placement.col, placement.direction);
                        placedWords.add(createPuzzleWord(word, 0, placement.row, placement.col, placement.direction));
                        usedWords.add(word.getWord());
                        placed = true;
                        attempts = 0;
                        break;
                    }
                }
            }

            if (!placed) {
                attempts++;
            }
        }

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

    private Word findFirstWord(int gridSize) {
        int maxLength = Math.min(gridSize - 2, 6);
        List<Word> words = wordRepository.findRandomWordsWithDefinitions(
            3, maxLength, PageRequest.of(0, 10)
        );
        if (words.isEmpty()) {
            return null;
        }
        return words.get(new Random().nextInt(words.size()));
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

    @Cacheable(value = "wordsContaining", key = "#candidate.character + '_' + #gridSize")
    private List<Word> getCachedWordsContaining(IntersectionCandidate candidate, int gridSize) {
        return wordRepository.findByContainingCharacterWithDefinitions(
            String.valueOf(candidate.character),
            2, gridSize,
            PageRequest.of(0, SEARCH_LIMIT)
        );
    }

    private Word findWordForIntersection(IntersectionCandidate candidate, int gridSize, Set<String> usedWords) {
        List<Word> words = getCachedWordsContaining(candidate, gridSize);

        return words.stream()
            .filter(w -> !usedWords.contains(w.getWord()))
            .filter(w -> w.getWord().indexOf(candidate.character) >= 0)
            .findFirst()
            .orElse(null);
    }

    private PlacementResult calculatePlacement(IntersectionCandidate candidate, Word word) {
        String wordStr = word.getWord();
        int charIndex = wordStr.indexOf(candidate.character);

        if (charIndex < 0) {
            return null;
        }

        if (candidate.direction == PuzzleWord.Direction.ACROSS) {
            int startCol = candidate.col - charIndex;
            return new PlacementResult(candidate.row, startCol, candidate.direction, 1);
        } else {
            int startRow = candidate.row - charIndex;
            return new PlacementResult(startRow, candidate.col, candidate.direction, 1);
        }
    }

    private boolean canPlaceWord(char[][] grid, String word, int startRow, int startCol, PuzzleWord.Direction direction, int gridSize) {
        int len = word.length();

        if (direction == PuzzleWord.Direction.ACROSS) {
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
                    // 빈 셀: 위/아래에 글자가 있으면 안됨 (나란히 붙기 방지)
                    if (startRow > 0 && grid[startRow - 1][col] != '\0') {
                        return false;
                    }
                    if (startRow < gridSize - 1 && grid[startRow + 1][col] != '\0') {
                        return false;
                    }
                }
            }
            return hasIntersection;

        } else {
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
                    // 빈 셀: 좌/우에 글자가 있으면 안됨 (나란히 붙기 방지)
                    if (startCol > 0 && grid[row][startCol - 1] != '\0') {
                        return false;
                    }
                    if (startCol < gridSize - 1 && grid[row][startCol + 1] != '\0') {
                        return false;
                    }
                }
            }
            return hasIntersection;
        }
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

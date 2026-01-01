package com.hakno.WordPuzzle.util;

import java.util.Arrays;

/**
 * 그리드 관련 유틸리티 함수
 * PuzzleGeneratorService에서 추출된 순수 함수들
 */
public final class GridUtils {

    // 자주 쓰이는 한글 글자들 (교차 가능성 높음)
    private static final String COMMON_CHARS = "가나다라마바사아자하이의를을에서는로고기대";

    private GridUtils() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    /**
     * 단어 수에 따라 적절한 그리드 크기를 계산
     * - 단어가 많을수록 더 큰 그리드 필요
     * - 최소 10, 최대 25
     *
     * @param wordCount 배치할 단어 수
     * @return 계산된 그리드 크기
     */
    public static int calculateGridSize(int wordCount) {
        // 기본 공식: 8 + (단어수 * 0.7)
        int size = (int) Math.round(8 + wordCount * 0.7);
        return Math.max(10, Math.min(25, size));
    }

    /**
     * 단어에 포함된 공통 글자 수를 계산
     * 공통 글자가 많을수록 교차 가능성이 높음
     *
     * @param word 검사할 단어
     * @return 공통 글자 수
     */
    public static int countCommonChars(String word) {
        if (word == null || word.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (char c : word.toCharArray()) {
            if (COMMON_CHARS.indexOf(c) >= 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 지정된 크기의 빈 그리드 생성
     *
     * @param size 그리드 크기
     * @return 빈 char 2차원 배열
     */
    public static char[][] createEmptyGrid(int size) {
        char[][] grid = new char[size][size];
        for (char[] row : grid) {
            Arrays.fill(row, '\0');
        }
        return grid;
    }

    /**
     * 공통 글자 목록 반환 (테스트용)
     */
    public static String getCommonChars() {
        return COMMON_CHARS;
    }
}

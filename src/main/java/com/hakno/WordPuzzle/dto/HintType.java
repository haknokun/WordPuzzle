package com.hakno.WordPuzzle.dto;

/**
 * 힌트 타입 Enum
 * 다양한 종류의 힌트를 제공하기 위한 타입 정의
 */
public enum HintType {
    /**
     * 초성 힌트 - 단어의 초성만 표시 (예: "사과" -> "ㅅㄱ")
     */
    CHOSUNG("초성"),

    /**
     * 예문 힌트 - 단어가 사용된 예문 제공
     */
    EXAMPLE("예문"),

    /**
     * 어원 힌트 - 단어의 어원/한자 정보 제공
     */
    ORIGIN("어원"),

    /**
     * 발음 힌트 - 단어의 발음 정보 제공
     */
    PRONUNCIATION("발음"),

    /**
     * 유의어 힌트 - 비슷한 의미의 단어 제공
     */
    SYNONYM("유의어"),

    /**
     * 반의어 힌트 - 반대 의미의 단어 제공
     */
    ANTONYM("반의어"),

    /**
     * 분야 힌트 - 단어가 속한 전문 분야 제공
     */
    CATEGORY("분야"),

    /**
     * 품사 힌트 - 단어의 품사 정보 제공
     */
    POS("품사"),

    /**
     * 단어유형 힌트 - 고유어/한자어/외래어/혼종어 정보 제공
     */
    WORD_TYPE("단어유형");

    private final String displayName;

    HintType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

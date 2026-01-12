package com.hakno.WordPuzzle.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 힌트 응답 DTO
 */
@Getter
@Builder
public class HintResponse {

    /**
     * 요청한 힌트 타입
     */
    private HintType hintType;

    /**
     * 힌트 타입의 표시명
     */
    private String hintTypeName;

    /**
     * 대상 단어 ID
     */
    private Long wordId;

    /**
     * 대상 단어
     */
    private String word;

    /**
     * 힌트 값 (단일 값인 경우)
     */
    private String value;

    /**
     * 힌트 값 목록 (여러 값인 경우: 예문, 유의어 등)
     */
    private List<String> values;

    /**
     * 힌트 사용 가능 여부
     */
    private boolean available;

    /**
     * 힌트가 없을 경우 메시지
     */
    private String message;

    /**
     * 단일 값 힌트 생성
     */
    public static HintResponse ofSingle(HintType type, Long wordId, String word, String value) {
        return HintResponse.builder()
                .hintType(type)
                .hintTypeName(type.getDisplayName())
                .wordId(wordId)
                .word(word)
                .value(value)
                .available(value != null && !value.isEmpty())
                .build();
    }

    /**
     * 다중 값 힌트 생성
     */
    public static HintResponse ofMultiple(HintType type, Long wordId, String word, List<String> values) {
        return HintResponse.builder()
                .hintType(type)
                .hintTypeName(type.getDisplayName())
                .wordId(wordId)
                .word(word)
                .values(values)
                .available(values != null && !values.isEmpty())
                .build();
    }

    /**
     * 힌트 없음 응답 생성
     */
    public static HintResponse notAvailable(HintType type, Long wordId, String word, String message) {
        return HintResponse.builder()
                .hintType(type)
                .hintTypeName(type != null ? type.getDisplayName() : null)
                .wordId(wordId)
                .word(word)
                .available(false)
                .message(message)
                .build();
    }
}

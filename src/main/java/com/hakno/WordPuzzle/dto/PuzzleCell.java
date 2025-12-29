package com.hakno.WordPuzzle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleCell {

    private int row;
    private int col;
    private String letter;

    @JsonProperty("isBlank")
    private boolean isBlank;

    private Integer acrossNumber;  // 가로 단어 시작 번호
    private Integer downNumber;    // 세로 단어 시작 번호
}

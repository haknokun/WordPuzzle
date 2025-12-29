package com.hakno.WordPuzzle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleWord {

    private Integer number;
    private String word;
    private String definition;
    private int startRow;
    private int startCol;
    private Direction direction;

    public enum Direction {
        ACROSS,  // 가로
        DOWN     // 세로
    }
}

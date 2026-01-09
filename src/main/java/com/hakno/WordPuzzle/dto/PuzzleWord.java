package com.hakno.WordPuzzle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleWord {

    private Integer number;
    private String word;
    private String definition;
    @Setter
    private int startRow;
    @Setter
    private int startCol;
    private Direction direction;

    public enum Direction {
        ACROSS,  // 가로
        DOWN     // 세로
    }
}

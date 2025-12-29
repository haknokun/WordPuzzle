package com.hakno.WordPuzzle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleResponse {

    private int gridSize;
    private List<List<PuzzleCell>> grid;
    private List<PuzzleWord> acrossWords;  // 가로 단어 힌트
    private List<PuzzleWord> downWords;    // 세로 단어 힌트
    private int totalWords;
}

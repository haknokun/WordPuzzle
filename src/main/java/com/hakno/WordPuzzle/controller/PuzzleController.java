package com.hakno.WordPuzzle.controller;

import com.hakno.WordPuzzle.dto.PuzzleResponse;
import com.hakno.WordPuzzle.service.PuzzleGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzle")
@RequiredArgsConstructor
public class PuzzleController {

    private final PuzzleGeneratorService puzzleGeneratorService;

    @GetMapping("/generate")
    public ResponseEntity<PuzzleResponse> generatePuzzle(
            @RequestParam(defaultValue = "15") int gridSize,
            @RequestParam(defaultValue = "10") int wordCount) {

        if (gridSize < 5 || gridSize > 30) {
            return ResponseEntity.badRequest().build();
        }
        if (wordCount < 3 || wordCount > 50) {
            return ResponseEntity.badRequest().build();
        }

        PuzzleResponse puzzle = puzzleGeneratorService.generatePuzzle(gridSize, wordCount);
        return ResponseEntity.ok(puzzle);
    }
}

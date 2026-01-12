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
            @RequestParam(required = false) Integer gridSize,
            @RequestParam(defaultValue = "10") int wordCount,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "default") String source,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String wordType) {

        if (wordCount < 3 || wordCount > 50) {
            return ResponseEntity.badRequest().build();
        }
        if (gridSize != null && (gridSize < 5 || gridSize > 30)) {
            return ResponseEntity.badRequest().build();
        }

        // category나 wordType이 지정되면 자동으로 std 소스 사용
        String effectiveSource = source;
        if (category != null || wordType != null) {
            effectiveSource = "std";
        }

        PuzzleResponse puzzle = puzzleGeneratorService.generatePuzzle(
                gridSize, wordCount, level, effectiveSource, category, wordType);
        return ResponseEntity.ok(puzzle);
    }
}

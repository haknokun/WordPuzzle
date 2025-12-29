package com.hakno.WordPuzzle.controller;

import com.hakno.WordPuzzle.dto.WordDto;
import com.hakno.WordPuzzle.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @GetMapping("/{id}")
    public ResponseEntity<WordDto> getById(@PathVariable Long id) {
        return wordService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<WordDto> searchByWord(@RequestParam String word) {
        return wordService.findByWord(word)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/length/{length}")
    public ResponseEntity<List<WordDto>> getByLength(@PathVariable Integer length) {
        return ResponseEntity.ok(wordService.findByLength(length));
    }

    @GetMapping("/random")
    public ResponseEntity<List<WordDto>> getRandom(
            @RequestParam Integer length,
            @RequestParam(defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(wordService.findRandomByLength(length, limit));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> count() {
        return ResponseEntity.ok(wordService.count());
    }
}

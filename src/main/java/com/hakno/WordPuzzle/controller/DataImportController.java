package com.hakno.WordPuzzle.controller;

import com.hakno.WordPuzzle.service.DataImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class DataImportController {

    private final DataImportService dataImportService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> importData(@RequestParam String path) {
        try {
            int count = dataImportService.importFromDirectory(path);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "importedCount", count,
                    "message", count + "개의 단어를 성공적으로 가져왔습니다."
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "파일 읽기 실패: " + e.getMessage()
            ));
        }
    }
}

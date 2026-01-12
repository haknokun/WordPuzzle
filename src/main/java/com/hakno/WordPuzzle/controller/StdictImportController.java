package com.hakno.WordPuzzle.controller;

/*
 * 표준국어대사전 API 임포트 기능 비활성화
 * API 사용 재개 시 주석 해제

import com.hakno.WordPuzzle.client.StdictApiClient;
import com.hakno.WordPuzzle.dto.ImportProgress;
import com.hakno.WordPuzzle.service.StdictImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stdict")
@RequiredArgsConstructor
public class StdictImportController {

    private final StdictImportService importService;
    private final StdictApiClient apiClient;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        boolean connected = apiClient.testConnection();
        return ResponseEntity.ok(Map.of(
                "connected", connected,
                "message", connected ? "API 연결 성공" : "API 연결 실패"
        ));
    }

    @PostMapping("/import/length/{length}")
    public ResponseEntity<Map<String, Object>> importByLength(@PathVariable int length) {
        if (length < 2 || length > 8) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "글자 수는 2~8 사이여야 합니다"
            ));
        }

        ImportProgress currentProgress = importService.getProgress();
        if (currentProgress.isRunning()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "이미 임포트가 진행 중입니다",
                    "progress", currentProgress
            ));
        }

        importService.importByLength(length);
        return ResponseEntity.ok(Map.of(
                "message", length + "글자 단어 임포트를 시작합니다",
                "status", "started"
        ));
    }

    @PostMapping("/import/start")
    public ResponseEntity<Map<String, Object>> startFullImport(
            @RequestParam(defaultValue = "2") int startLength,
            @RequestParam(defaultValue = "8") int endLength) {

        ImportProgress currentProgress = importService.getProgress();
        if (currentProgress.isRunning()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "이미 임포트가 진행 중입니다",
                    "progress", currentProgress
            ));
        }

        importService.importByLength(startLength);

        return ResponseEntity.ok(Map.of(
                "message", String.format("%d~%d글자 단어 임포트를 시작합니다", startLength, endLength),
                "status", "started",
                "note", "각 글자 수별로 순차적으로 진행됩니다. /progress API로 진행 상황을 확인하세요."
        ));
    }

    @GetMapping("/import/progress")
    public ResponseEntity<ImportProgress> getProgress() {
        return ResponseEntity.ok(importService.getProgress());
    }

    @PostMapping("/import/stop")
    public ResponseEntity<Map<String, Object>> stopImport() {
        importService.stopImport();
        return ResponseEntity.ok(Map.of(
                "message", "임포트 중단 요청됨",
                "status", "stopping"
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<StdictImportService.ImportStats> getStats() {
        return ResponseEntity.ok(importService.getStats());
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int start,
            @RequestParam(defaultValue = "10") int num) {
        try {
            return ResponseEntity.ok(apiClient.search(q, start, num));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/view/{targetCode}")
    public ResponseEntity<?> view(@PathVariable String targetCode) {
        try {
            return ResponseEntity.ok(apiClient.getWordDetail(targetCode));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
*/

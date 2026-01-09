package com.hakno.WordPuzzle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportProgress {
    private int totalExpected;      // 예상 총 개수
    private int imported;           // 임포트된 개수
    private int failed;             // 실패한 개수
    private int skipped;            // 중복으로 스킵된 개수
    private String currentPhase;    // 현재 단계 (예: "2글자 단어 임포트 중")
    private boolean running;        // 실행 중 여부
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String lastError;       // 마지막 에러 메시지

    public double getProgressPercent() {
        if (totalExpected == 0) return 0;
        return (double) (imported + skipped) / totalExpected * 100;
    }

    public long getElapsedSeconds() {
        if (startTime == null) return 0;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }
}

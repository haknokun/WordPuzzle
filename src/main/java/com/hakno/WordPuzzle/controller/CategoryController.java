package com.hakno.WordPuzzle.controller;

import com.hakno.WordPuzzle.repository.StdWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 카테고리 및 단어 유형 API 컨트롤러
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {

    private final StdWordRepository stdWordRepository;

    // 단어 유형 상수 (고유어, 한자어, 외래어, 혼종어)
    private static final List<Map<String, String>> WORD_TYPES = List.of(
            Map.of("code", "고유어", "name", "고유어", "description", "순우리말"),
            Map.of("code", "한자어", "name", "한자어", "description", "한자에서 유래한 말"),
            Map.of("code", "외래어", "name", "외래어", "description", "외국에서 들어온 말"),
            Map.of("code", "혼종어", "name", "혼종어", "description", "여러 어원이 섞인 말")
    );

    /**
     * 전문 분야(카테고리) 목록 조회
     * 표준국어대사전 sense의 category 필드에서 추출
     *
     * @return 카테고리 목록
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = stdWordRepository.findAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * 카테고리 목록 (상세 정보 포함)
     *
     * @return 카테고리 목록 (코드, 이름, 단어 수)
     */
    @GetMapping("/categories/detailed")
    public ResponseEntity<List<Map<String, Object>>> getCategoriesDetailed() {
        List<String> categories = stdWordRepository.findAllCategories();

        List<Map<String, Object>> result = categories.stream()
                .map(cat -> Map.<String, Object>of(
                        "code", cat,
                        "name", cat
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * 단어 유형 목록 조회
     * 고유어, 한자어, 외래어, 혼종어
     *
     * @return 단어 유형 목록
     */
    @GetMapping("/word-types")
    public ResponseEntity<List<Map<String, String>>> getWordTypes() {
        return ResponseEntity.ok(WORD_TYPES);
    }

    /**
     * 단어 유형별 통계 조회
     *
     * @return 단어 유형별 단어 수
     */
    @GetMapping("/word-types/stats")
    public ResponseEntity<List<Map<String, Object>>> getWordTypeStats() {
        List<Object[]> stats = stdWordRepository.countByWordType();

        List<Map<String, Object>> result = stats.stream()
                .map(row -> Map.<String, Object>of(
                        "wordType", row[0] != null ? row[0].toString() : "미분류",
                        "count", ((Number) row[1]).longValue()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }
}

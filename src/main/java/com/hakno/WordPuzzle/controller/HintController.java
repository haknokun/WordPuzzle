package com.hakno.WordPuzzle.controller;

import com.hakno.WordPuzzle.dto.HintResponse;
import com.hakno.WordPuzzle.dto.HintType;
import com.hakno.WordPuzzle.service.HintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 힌트 API 컨트롤러
 */
@RestController
@RequestMapping("/api/puzzle/hint")
@RequiredArgsConstructor
@Slf4j
public class HintController {

    private final HintService hintService;

    /**
     * 특정 타입의 힌트 조회
     *
     * @param wordId 단어 ID
     * @param type   힌트 타입 (CHOSUNG, EXAMPLE, ORIGIN, PRONUNCIATION, SYNONYM, ANTONYM, CATEGORY, POS, WORD_TYPE)
     * @return 힌트 응답
     */
    @GetMapping
    public ResponseEntity<HintResponse> getHint(
            @RequestParam Long wordId,
            @RequestParam String type) {

        HintType hintType;
        try {
            hintType = HintType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid hint type requested: {}", type);
            return ResponseEntity.badRequest().body(
                    HintResponse.notAvailable(null, wordId, null,
                            "유효하지 않은 힌트 타입입니다: " + type)
            );
        }

        HintResponse response = hintService.getHint(wordId, hintType);
        return ResponseEntity.ok(response);
    }

    /**
     * 단어의 모든 힌트 조회
     *
     * @param wordId 단어 ID
     * @return 모든 힌트 목록
     */
    @GetMapping("/all")
    public ResponseEntity<List<HintResponse>> getAllHints(@RequestParam Long wordId) {
        List<HintResponse> hints = hintService.getAllHints(wordId);
        return ResponseEntity.ok(hints);
    }

    /**
     * 단어의 사용 가능한 힌트만 조회
     *
     * @param wordId 단어 ID
     * @return 사용 가능한 힌트 목록
     */
    @GetMapping("/available")
    public ResponseEntity<List<HintResponse>> getAvailableHints(@RequestParam Long wordId) {
        List<HintResponse> hints = hintService.getAvailableHints(wordId);
        return ResponseEntity.ok(hints);
    }

    /**
     * 초성 힌트 조회 (자주 사용되므로 별도 엔드포인트)
     *
     * @param wordId 단어 ID
     * @return 초성 힌트
     */
    @GetMapping("/chosung")
    public ResponseEntity<HintResponse> getChosungHint(@RequestParam Long wordId) {
        HintResponse response = hintService.getHint(wordId, HintType.CHOSUNG);
        return ResponseEntity.ok(response);
    }

    /**
     * 예문 힌트 조회
     *
     * @param wordId 단어 ID
     * @return 예문 힌트
     */
    @GetMapping("/example")
    public ResponseEntity<HintResponse> getExampleHint(@RequestParam Long wordId) {
        HintResponse response = hintService.getHint(wordId, HintType.EXAMPLE);
        return ResponseEntity.ok(response);
    }

    /**
     * 어원 힌트 조회
     *
     * @param wordId 단어 ID
     * @return 어원 힌트
     */
    @GetMapping("/origin")
    public ResponseEntity<HintResponse> getOriginHint(@RequestParam Long wordId) {
        HintResponse response = hintService.getHint(wordId, HintType.ORIGIN);
        return ResponseEntity.ok(response);
    }

    /**
     * 유의어 힌트 조회
     *
     * @param wordId 단어 ID
     * @return 유의어 힌트
     */
    @GetMapping("/synonym")
    public ResponseEntity<HintResponse> getSynonymHint(@RequestParam Long wordId) {
        HintResponse response = hintService.getHint(wordId, HintType.SYNONYM);
        return ResponseEntity.ok(response);
    }

    /**
     * 반의어 힌트 조회
     *
     * @param wordId 단어 ID
     * @return 반의어 힌트
     */
    @GetMapping("/antonym")
    public ResponseEntity<HintResponse> getAntonymHint(@RequestParam Long wordId) {
        HintResponse response = hintService.getHint(wordId, HintType.ANTONYM);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용 가능한 힌트 타입 목록 조회
     *
     * @return 힌트 타입 목록
     */
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> getHintTypes() {
        List<Map<String, String>> types = Arrays.stream(HintType.values())
                .map(type -> Map.of(
                        "type", type.name(),
                        "displayName", type.getDisplayName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(types);
    }
}

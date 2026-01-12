package com.hakno.WordPuzzle.integration;

import com.hakno.WordPuzzle.controller.PuzzleController;
import com.hakno.WordPuzzle.dto.PuzzleCell;
import com.hakno.WordPuzzle.dto.PuzzleResponse;
import com.hakno.WordPuzzle.dto.PuzzleWord;
import com.hakno.WordPuzzle.service.PuzzleGeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PuzzleController 단위 테스트
 * @WebMvcTest를 사용하여 Controller 레이어만 테스트합니다.
 * PuzzleGeneratorService는 Mock으로 대체됩니다.
 */
@WebMvcTest(PuzzleController.class)
@DisplayName("PuzzleController 테스트")
class PuzzleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PuzzleGeneratorService puzzleGeneratorService;

    private PuzzleResponse createMockPuzzleResponse(int gridSize) {
        // Mock 그리드 생성
        List<List<PuzzleCell>> grid = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) {
            List<PuzzleCell> row = new ArrayList<>();
            for (int j = 0; j < gridSize; j++) {
                row.add(PuzzleCell.builder()
                        .row(i)
                        .col(j)
                        .letter("")
                        .isBlank(true)
                        .build());
            }
            grid.add(row);
        }

        // Mock 단어 생성
        List<PuzzleWord> acrossWords = List.of(
                PuzzleWord.builder()
                        .number(1)
                        .word("테스트")
                        .definition("테스트 정의")
                        .startRow(0)
                        .startCol(0)
                        .direction(PuzzleWord.Direction.ACROSS)
                        .build()
        );

        List<PuzzleWord> downWords = List.of(
                PuzzleWord.builder()
                        .number(1)
                        .word("단어")
                        .definition("단어 정의")
                        .startRow(0)
                        .startCol(0)
                        .direction(PuzzleWord.Direction.DOWN)
                        .build()
        );

        return PuzzleResponse.builder()
                .gridSize(gridSize)
                .grid(grid)
                .acrossWords(acrossWords)
                .downWords(downWords)
                .totalWords(2)
                .build();
    }

    @Nested
    @DisplayName("GET /api/puzzle/generate - 정상 케이스")
    class GeneratePuzzleSuccessTests {

        @Test
        @DisplayName("기본 파라미터로 퍼즐 생성")
        void generatePuzzle_defaultParams_returnsPuzzle() throws Exception {
            // given
            when(puzzleGeneratorService.generatePuzzle(isNull(), eq(5), isNull(), eq("default"), isNull(), isNull()))
                    .thenReturn(createMockPuzzleResponse(12));

            // when & then
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("wordCount", "5"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.gridSize").value(12))
                    .andExpect(jsonPath("$.grid").isArray())
                    .andExpect(jsonPath("$.acrossWords").isArray())
                    .andExpect(jsonPath("$.downWords").isArray());
        }

        @Test
        @DisplayName("gridSize 지정하여 퍼즐 생성")
        void generatePuzzle_withGridSize_returnsPuzzleWithSize() throws Exception {
            // given
            when(puzzleGeneratorService.generatePuzzle(eq(15), eq(5), isNull(), eq("default"), isNull(), isNull()))
                    .thenReturn(createMockPuzzleResponse(15));

            // when & then
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("gridSize", "15")
                            .param("wordCount", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gridSize").value(15));
        }

        @Test
        @DisplayName("난이도 지정하여 퍼즐 생성 - 초급")
        void generatePuzzle_withBeginnerLevel_returnsPuzzle() throws Exception {
            // given
            when(puzzleGeneratorService.generatePuzzle(isNull(), eq(5), eq("초급"), eq("default"), isNull(), isNull()))
                    .thenReturn(createMockPuzzleResponse(12));

            // when & then
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("wordCount", "5")
                            .param("level", "초급"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.acrossWords").isNotEmpty());
        }

        @Test
        @DisplayName("퍼즐 응답에 필수 필드가 모두 포함됨")
        void generatePuzzle_responseContainsAllRequiredFields() throws Exception {
            // given
            when(puzzleGeneratorService.generatePuzzle(isNull(), eq(5), isNull(), eq("default"), isNull(), isNull()))
                    .thenReturn(createMockPuzzleResponse(12));

            // when & then
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("wordCount", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gridSize").exists())
                    .andExpect(jsonPath("$.grid").exists())
                    .andExpect(jsonPath("$.acrossWords").exists())
                    .andExpect(jsonPath("$.downWords").exists())
                    .andExpect(jsonPath("$.totalWords").exists());
        }

        @Test
        @DisplayName("생성된 단어에 definition 포함")
        void generatePuzzle_wordsContainDefinitions() throws Exception {
            // given
            when(puzzleGeneratorService.generatePuzzle(isNull(), eq(5), isNull(), eq("default"), isNull(), isNull()))
                    .thenReturn(createMockPuzzleResponse(12));

            // when & then
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("wordCount", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.acrossWords[0].definition").exists())
                    .andExpect(jsonPath("$.acrossWords[0].definition").value("테스트 정의"));
        }
    }

    @Nested
    @DisplayName("GET /api/puzzle/generate - 파라미터 검증")
    class GeneratePuzzleValidationTests {

        @Test
        @DisplayName("wordCount가 3 미만일 때 400 에러")
        void generatePuzzle_wordCountTooLow_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("wordCount", "2"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("wordCount가 50 초과일 때 400 에러")
        void generatePuzzle_wordCountTooHigh_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("wordCount", "51"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("gridSize가 5 미만일 때 400 에러")
        void generatePuzzle_gridSizeTooSmall_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("gridSize", "4")
                            .param("wordCount", "5"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("gridSize가 30 초과일 때 400 에러")
        void generatePuzzle_gridSizeTooLarge_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("gridSize", "31")
                            .param("wordCount", "5"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("wordCount 경계값 테스트 - 최소값 3")
        void generatePuzzle_wordCountMinBoundary_returnsOk() throws Exception {
            // given
            when(puzzleGeneratorService.generatePuzzle(isNull(), eq(3), isNull(), eq("default"), isNull(), isNull()))
                    .thenReturn(createMockPuzzleResponse(10));

            // when & then
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("wordCount", "3"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("wordCount 경계값 테스트 - 최대값 50")
        void generatePuzzle_wordCountMaxBoundary_returnsOk() throws Exception {
            // given
            when(puzzleGeneratorService.generatePuzzle(isNull(), eq(50), isNull(), eq("default"), isNull(), isNull()))
                    .thenReturn(createMockPuzzleResponse(25));

            // when & then
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("wordCount", "50"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("gridSize 경계값 테스트 - 최소값 5")
        void generatePuzzle_gridSizeMinBoundary_returnsOk() throws Exception {
            // given
            when(puzzleGeneratorService.generatePuzzle(eq(5), eq(3), isNull(), eq("default"), isNull(), isNull()))
                    .thenReturn(createMockPuzzleResponse(5));

            // when & then
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("gridSize", "5")
                            .param("wordCount", "3"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("gridSize 경계값 테스트 - 최대값 30")
        void generatePuzzle_gridSizeMaxBoundary_returnsOk() throws Exception {
            // given
            when(puzzleGeneratorService.generatePuzzle(eq(30), eq(5), isNull(), eq("default"), isNull(), isNull()))
                    .thenReturn(createMockPuzzleResponse(30));

            // when & then
            mockMvc.perform(get("/api/puzzle/generate")
                            .param("gridSize", "30")
                            .param("wordCount", "5"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/puzzle/generate - 기본값 테스트")
    class DefaultValueTests {

        @Test
        @DisplayName("wordCount 기본값은 10")
        void generatePuzzle_noWordCount_usesDefault10() throws Exception {
            // given
            when(puzzleGeneratorService.generatePuzzle(isNull(), eq(10), isNull(), eq("default"), isNull(), isNull()))
                    .thenReturn(createMockPuzzleResponse(15));

            // when & then
            mockMvc.perform(get("/api/puzzle/generate"))
                    .andExpect(status().isOk());
        }
    }
}

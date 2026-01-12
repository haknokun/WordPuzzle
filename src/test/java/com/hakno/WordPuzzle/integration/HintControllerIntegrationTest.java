package com.hakno.WordPuzzle.integration;

import com.hakno.WordPuzzle.controller.HintController;
import com.hakno.WordPuzzle.dto.HintResponse;
import com.hakno.WordPuzzle.dto.HintType;
import com.hakno.WordPuzzle.service.HintService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HintController 통합 테스트
 * Task 22: 힌트 시스템 확장
 */
@WebMvcTest(HintController.class)
@DisplayName("HintController 테스트")
class HintControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HintService hintService;

    @Nested
    @DisplayName("GET /api/puzzle/hint - 힌트 조회")
    class GetHintTests {

        @Test
        @DisplayName("초성 힌트를 성공적으로 조회한다")
        void getChosungHintSuccess() throws Exception {
            HintResponse response = HintResponse.ofSingle(
                    HintType.CHOSUNG, 1L, "사과", "ㅅㄱ");
            when(hintService.getHint(eq(1L), eq(HintType.CHOSUNG))).thenReturn(response);

            mockMvc.perform(get("/api/puzzle/hint")
                            .param("wordId", "1")
                            .param("type", "CHOSUNG"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.hintType").value("CHOSUNG"))
                    .andExpect(jsonPath("$.value").value("ㅅㄱ"))
                    .andExpect(jsonPath("$.available").value(true));
        }

        @Test
        @DisplayName("소문자 힌트 타입도 처리한다")
        void getLowercaseHintType() throws Exception {
            HintResponse response = HintResponse.ofSingle(
                    HintType.ORIGIN, 1L, "사과", "沙果");
            when(hintService.getHint(eq(1L), eq(HintType.ORIGIN))).thenReturn(response);

            mockMvc.perform(get("/api/puzzle/hint")
                            .param("wordId", "1")
                            .param("type", "origin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hintType").value("ORIGIN"))
                    .andExpect(jsonPath("$.value").value("沙果"));
        }

        @Test
        @DisplayName("유효하지 않은 힌트 타입은 400 에러를 반환한다")
        void invalidHintTypeReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/puzzle/hint")
                            .param("wordId", "1")
                            .param("type", "INVALID_TYPE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.available").value(false))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("다중 값 힌트를 조회한다")
        void getMultipleValueHint() throws Exception {
            HintResponse response = HintResponse.ofMultiple(
                    HintType.SYNONYM, 1L, "사과",
                    List.of("능금", "평과"));
            when(hintService.getHint(eq(1L), eq(HintType.SYNONYM))).thenReturn(response);

            mockMvc.perform(get("/api/puzzle/hint")
                            .param("wordId", "1")
                            .param("type", "SYNONYM"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hintType").value("SYNONYM"))
                    .andExpect(jsonPath("$.values").isArray())
                    .andExpect(jsonPath("$.values[0]").value("능금"))
                    .andExpect(jsonPath("$.values[1]").value("평과"));
        }
    }

    @Nested
    @DisplayName("GET /api/puzzle/hint/chosung - 초성 힌트 전용 엔드포인트")
    class GetChosungHintTests {

        @Test
        @DisplayName("초성 힌트를 조회한다")
        void getChosungHint() throws Exception {
            HintResponse response = HintResponse.ofSingle(
                    HintType.CHOSUNG, 1L, "대한민국", "ㄷㅎㅁㄱ");
            when(hintService.getHint(eq(1L), eq(HintType.CHOSUNG))).thenReturn(response);

            mockMvc.perform(get("/api/puzzle/hint/chosung")
                            .param("wordId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.value").value("ㄷㅎㅁㄱ"));
        }
    }

    @Nested
    @DisplayName("GET /api/puzzle/hint/all - 모든 힌트 조회")
    class GetAllHintsTests {

        @Test
        @DisplayName("모든 힌트를 조회한다")
        void getAllHints() throws Exception {
            List<HintResponse> responses = List.of(
                    HintResponse.ofSingle(HintType.CHOSUNG, 1L, "사과", "ㅅㄱ"),
                    HintResponse.ofSingle(HintType.ORIGIN, 1L, "사과", "沙果"),
                    HintResponse.notAvailable(HintType.SYNONYM, 1L, "사과", "유의어가 없습니다")
            );
            when(hintService.getAllHints(eq(1L))).thenReturn(responses);

            mockMvc.perform(get("/api/puzzle/hint/all")
                            .param("wordId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3));
        }
    }

    @Nested
    @DisplayName("GET /api/puzzle/hint/available - 사용 가능한 힌트 조회")
    class GetAvailableHintsTests {

        @Test
        @DisplayName("사용 가능한 힌트만 조회한다")
        void getAvailableHints() throws Exception {
            List<HintResponse> responses = List.of(
                    HintResponse.ofSingle(HintType.CHOSUNG, 1L, "사과", "ㅅㄱ"),
                    HintResponse.ofSingle(HintType.ORIGIN, 1L, "사과", "沙果")
            );
            when(hintService.getAvailableHints(eq(1L))).thenReturn(responses);

            mockMvc.perform(get("/api/puzzle/hint/available")
                            .param("wordId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].available").value(true))
                    .andExpect(jsonPath("$[1].available").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/puzzle/hint/types - 힌트 타입 목록 조회")
    class GetHintTypesTests {

        @Test
        @DisplayName("모든 힌트 타입 목록을 조회한다")
        void getHintTypes() throws Exception {
            mockMvc.perform(get("/api/puzzle/hint/types"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(HintType.values().length))
                    .andExpect(jsonPath("$[0].type").exists())
                    .andExpect(jsonPath("$[0].displayName").exists());
        }
    }

    @Nested
    @DisplayName("개별 힌트 엔드포인트 테스트")
    class IndividualHintEndpointsTests {

        @Test
        @DisplayName("예문 힌트를 조회한다")
        void getExampleHint() throws Exception {
            HintResponse response = HintResponse.ofMultiple(
                    HintType.EXAMPLE, 1L, "사과",
                    List.of("빨간 사과가 맛있다."));
            when(hintService.getHint(eq(1L), eq(HintType.EXAMPLE))).thenReturn(response);

            mockMvc.perform(get("/api/puzzle/hint/example")
                            .param("wordId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hintType").value("EXAMPLE"))
                    .andExpect(jsonPath("$.values[0]").value("빨간 사과가 맛있다."));
        }

        @Test
        @DisplayName("어원 힌트를 조회한다")
        void getOriginHint() throws Exception {
            HintResponse response = HintResponse.ofSingle(
                    HintType.ORIGIN, 1L, "사과", "沙果");
            when(hintService.getHint(eq(1L), eq(HintType.ORIGIN))).thenReturn(response);

            mockMvc.perform(get("/api/puzzle/hint/origin")
                            .param("wordId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hintType").value("ORIGIN"))
                    .andExpect(jsonPath("$.value").value("沙果"));
        }

        @Test
        @DisplayName("유의어 힌트를 조회한다")
        void getSynonymHint() throws Exception {
            HintResponse response = HintResponse.ofMultiple(
                    HintType.SYNONYM, 1L, "사과", List.of("능금"));
            when(hintService.getHint(eq(1L), eq(HintType.SYNONYM))).thenReturn(response);

            mockMvc.perform(get("/api/puzzle/hint/synonym")
                            .param("wordId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hintType").value("SYNONYM"));
        }

        @Test
        @DisplayName("반의어 힌트를 조회한다")
        void getAntonymHint() throws Exception {
            HintResponse response = HintResponse.notAvailable(
                    HintType.ANTONYM, 1L, "사과", "반의어가 없습니다");
            when(hintService.getHint(eq(1L), eq(HintType.ANTONYM))).thenReturn(response);

            mockMvc.perform(get("/api/puzzle/hint/antonym")
                            .param("wordId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hintType").value("ANTONYM"))
                    .andExpect(jsonPath("$.available").value(false));
        }
    }
}

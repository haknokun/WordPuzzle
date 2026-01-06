package com.hakno.WordPuzzle.integration;

import com.hakno.WordPuzzle.controller.DataImportController;
import com.hakno.WordPuzzle.service.DataImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DataImportController 통합 테스트
 * Task 3: Backend DataImportController 테스트 추가
 */
@WebMvcTest(DataImportController.class)
@DisplayName("DataImportController 테스트")
class DataImportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataImportService dataImportService;

    @Nested
    @DisplayName("POST /api/import - 성공 케이스")
    class ImportDataSuccessTests {

        @Test
        @DisplayName("데이터 import 성공 시 200 OK와 결과 반환")
        void importData_success_returnsOkWithCount() throws Exception {
            // given
            when(dataImportService.importFromDirectory("/data/words")).thenReturn(100);

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", "/data/words"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.importedCount").value(100))
                    .andExpect(jsonPath("$.message").value("100개의 단어를 성공적으로 가져왔습니다."));
        }

        @Test
        @DisplayName("0개 단어 import 시에도 성공 응답")
        void importData_zeroWords_returnsOkWithZeroCount() throws Exception {
            // given
            when(dataImportService.importFromDirectory("/empty/dir")).thenReturn(0);

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", "/empty/dir"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.importedCount").value(0))
                    .andExpect(jsonPath("$.message").value("0개의 단어를 성공적으로 가져왔습니다."));
        }

        @Test
        @DisplayName("대량 데이터 import 성공")
        void importData_largeCount_returnsOkWithCount() throws Exception {
            // given
            when(dataImportService.importFromDirectory("/data/large")).thenReturn(7417);

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", "/data/large"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.importedCount").value(7417));
        }

        @Test
        @DisplayName("응답에 필수 필드가 모두 포함됨")
        void importData_success_containsAllRequiredFields() throws Exception {
            // given
            when(dataImportService.importFromDirectory(anyString())).thenReturn(50);

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", "/any/path"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").exists())
                    .andExpect(jsonPath("$.importedCount").exists())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/import - 에러 케이스")
    class ImportDataErrorTests {

        @Test
        @DisplayName("파일 읽기 실패 시 400 Bad Request")
        void importData_ioException_returnsBadRequest() throws Exception {
            // given
            when(dataImportService.importFromDirectory("/invalid/path"))
                    .thenThrow(new IOException("디렉토리를 찾을 수 없습니다"));

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", "/invalid/path"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("파일 읽기 실패: 디렉토리를 찾을 수 없습니다"));
        }

        @Test
        @DisplayName("권한 없는 경로 접근 시 400 Bad Request")
        void importData_accessDenied_returnsBadRequest() throws Exception {
            // given
            when(dataImportService.importFromDirectory("/restricted/path"))
                    .thenThrow(new IOException("Access denied"));

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", "/restricted/path"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("빈 경로 파라미터 처리")
        void importData_emptyPath_handlesGracefully() throws Exception {
            // given
            when(dataImportService.importFromDirectory(""))
                    .thenThrow(new IOException("경로가 비어있습니다"));

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("에러 응답에 필수 필드가 모두 포함됨")
        void importData_error_containsAllRequiredFields() throws Exception {
            // given
            when(dataImportService.importFromDirectory(anyString()))
                    .thenThrow(new IOException("테스트 에러"));

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", "/error/path"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("특수문자 포함 경로 처리")
        void importData_specialCharPath_handlesCorrectly() throws Exception {
            // given
            String specialPath = "/path/with spaces/한글경로";
            when(dataImportService.importFromDirectory(specialPath))
                    .thenThrow(new IOException("Invalid path"));

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", specialPath))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/import - 파라미터 검증")
    class ImportDataParameterTests {

        @Test
        @DisplayName("path 파라미터 없이 호출 시 에러")
        void importData_missingPath_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/import"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("절대 경로로 import 요청")
        void importData_absolutePath_callsServiceCorrectly() throws Exception {
            // given
            String absolutePath = "C:/Users/data/words";
            when(dataImportService.importFromDirectory(absolutePath)).thenReturn(10);

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", absolutePath))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("상대 경로로 import 요청")
        void importData_relativePath_callsServiceCorrectly() throws Exception {
            // given
            String relativePath = "./data/words";
            when(dataImportService.importFromDirectory(relativePath)).thenReturn(5);

            // when & then
            mockMvc.perform(post("/api/import")
                            .param("path", relativePath))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}

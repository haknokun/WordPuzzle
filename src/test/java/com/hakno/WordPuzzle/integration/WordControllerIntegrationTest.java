package com.hakno.WordPuzzle.integration;

import com.hakno.WordPuzzle.controller.WordController;
import com.hakno.WordPuzzle.dto.DefinitionDto;
import com.hakno.WordPuzzle.dto.WordDto;
import com.hakno.WordPuzzle.service.WordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WordController 통합 테스트
 * Task 2: Backend WordController 테스트 추가
 */
@WebMvcTest(WordController.class)
@DisplayName("WordController 테스트")
class WordControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WordService wordService;

    /**
     * 테스트용 WordDto 생성
     */
    private WordDto createMockWordDto(Long id, String word) {
        return WordDto.builder()
                .id(id)
                .word(word)
                .length(word.length())
                .definitions(List.of(
                        DefinitionDto.builder()
                                .senseOrder(1)
                                .definition(word + "의 정의")
                                .build()
                ))
                .build();
    }

    @Nested
    @DisplayName("GET /api/words/{id} - ID로 단어 조회")
    class GetByIdTests {

        @Test
        @DisplayName("존재하는 ID로 조회 시 200 OK와 단어 반환")
        void getById_existingId_returnsWord() throws Exception {
            // given
            WordDto wordDto = createMockWordDto(1L, "사과");
            when(wordService.findById(1L)).thenReturn(Optional.of(wordDto));

            // when & then
            mockMvc.perform(get("/api/words/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.word").value("사과"))
                    .andExpect(jsonPath("$.length").value(2))
                    .andExpect(jsonPath("$.definitions").isArray())
                    .andExpect(jsonPath("$.definitions[0].definition").value("사과의 정의"));
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 404 Not Found")
        void getById_nonExistingId_returnsNotFound() throws Exception {
            // given
            when(wordService.findById(999L)).thenReturn(Optional.empty());

            // when & then
            mockMvc.perform(get("/api/words/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("정의가 여러 개인 단어 조회")
        void getById_multipleDefinitions_returnsAllDefinitions() throws Exception {
            // given
            WordDto wordDto = WordDto.builder()
                    .id(1L)
                    .word("배")
                    .length(1)
                    .definitions(List.of(
                            DefinitionDto.builder().senseOrder(1).definition("과일의 한 종류").build(),
                            DefinitionDto.builder().senseOrder(2).definition("물 위를 다니는 탈것").build(),
                            DefinitionDto.builder().senseOrder(3).definition("신체 부위").build()
                    ))
                    .build();
            when(wordService.findById(1L)).thenReturn(Optional.of(wordDto));

            // when & then
            mockMvc.perform(get("/api/words/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.definitions.length()").value(3))
                    .andExpect(jsonPath("$.definitions[0].definition").value("과일의 한 종류"))
                    .andExpect(jsonPath("$.definitions[1].definition").value("물 위를 다니는 탈것"));
        }
    }

    @Nested
    @DisplayName("GET /api/words/search - 단어로 검색")
    class SearchByWordTests {

        @Test
        @DisplayName("존재하는 단어 검색 시 200 OK와 단어 반환")
        void searchByWord_existingWord_returnsWord() throws Exception {
            // given
            WordDto wordDto = createMockWordDto(1L, "사과");
            when(wordService.findByWord("사과")).thenReturn(Optional.of(wordDto));

            // when & then
            mockMvc.perform(get("/api/words/search")
                            .param("word", "사과"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.word").value("사과"));
        }

        @Test
        @DisplayName("존재하지 않는 단어 검색 시 404 Not Found")
        void searchByWord_nonExistingWord_returnsNotFound() throws Exception {
            // given
            when(wordService.findByWord("없는단어")).thenReturn(Optional.empty());

            // when & then
            mockMvc.perform(get("/api/words/search")
                            .param("word", "없는단어"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("한글 단어 검색")
        void searchByWord_koreanWord_returnsWord() throws Exception {
            // given
            WordDto wordDto = createMockWordDto(1L, "대한민국");
            when(wordService.findByWord("대한민국")).thenReturn(Optional.of(wordDto));

            // when & then
            mockMvc.perform(get("/api/words/search")
                            .param("word", "대한민국"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.word").value("대한민국"))
                    .andExpect(jsonPath("$.length").value(4));
        }
    }

    @Nested
    @DisplayName("GET /api/words/length/{length} - 길이로 단어 조회")
    class GetByLengthTests {

        @Test
        @DisplayName("특정 길이의 단어 목록 조회")
        void getByLength_existingLength_returnsWordList() throws Exception {
            // given
            List<WordDto> words = List.of(
                    createMockWordDto(1L, "사과"),
                    createMockWordDto(2L, "바나나".substring(0, 2)), // 2글자
                    createMockWordDto(3L, "포도")
            );
            when(wordService.findByLength(2)).thenReturn(words);

            // when & then
            mockMvc.perform(get("/api/words/length/2"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].word").value("사과"));
        }

        @Test
        @DisplayName("해당 길이의 단어가 없을 때 빈 배열 반환")
        void getByLength_noWords_returnsEmptyArray() throws Exception {
            // given
            when(wordService.findByLength(100)).thenReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/words/length/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("길이 1인 단어 조회")
        void getByLength_lengthOne_returnsWords() throws Exception {
            // given
            List<WordDto> words = List.of(
                    WordDto.builder().id(1L).word("나").length(1).definitions(Collections.emptyList()).build()
            );
            when(wordService.findByLength(1)).thenReturn(words);

            // when & then
            mockMvc.perform(get("/api/words/length/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].length").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/words/random - 랜덤 단어 조회")
    class GetRandomTests {

        @Test
        @DisplayName("특정 길이의 랜덤 단어 조회 - 기본 limit 10")
        void getRandom_withLength_returnsRandomWords() throws Exception {
            // given
            List<WordDto> words = List.of(
                    createMockWordDto(1L, "사과"),
                    createMockWordDto(2L, "배추"),
                    createMockWordDto(3L, "포도")
            );
            when(wordService.findRandomByLength(2, 10)).thenReturn(words);

            // when & then
            mockMvc.perform(get("/api/words/random")
                            .param("length", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        @DisplayName("limit 지정하여 랜덤 단어 조회")
        void getRandom_withLengthAndLimit_returnsLimitedWords() throws Exception {
            // given
            List<WordDto> words = List.of(
                    createMockWordDto(1L, "사과"),
                    createMockWordDto(2L, "배추")
            );
            when(wordService.findRandomByLength(2, 2)).thenReturn(words);

            // when & then
            mockMvc.perform(get("/api/words/random")
                            .param("length", "2")
                            .param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("랜덤 단어가 없을 때 빈 배열 반환")
        void getRandom_noWords_returnsEmptyArray() throws Exception {
            // given
            when(wordService.findRandomByLength(100, 10)).thenReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/words/random")
                            .param("length", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("limit=1로 단일 랜덤 단어 조회")
        void getRandom_limitOne_returnsSingleWord() throws Exception {
            // given
            List<WordDto> words = List.of(createMockWordDto(1L, "사과"));
            when(wordService.findRandomByLength(2, 1)).thenReturn(words);

            // when & then
            mockMvc.perform(get("/api/words/random")
                            .param("length", "2")
                            .param("limit", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/words/count - 전체 단어 수 조회")
    class CountTests {

        @Test
        @DisplayName("전체 단어 수 조회")
        void count_returnsWordCount() throws Exception {
            // given
            when(wordService.count()).thenReturn(7417L);

            // when & then
            mockMvc.perform(get("/api/words/count"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().string("7417"));
        }

        @Test
        @DisplayName("단어가 없을 때 0 반환")
        void count_noWords_returnsZero() throws Exception {
            // given
            when(wordService.count()).thenReturn(0L);

            // when & then
            mockMvc.perform(get("/api/words/count"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("0"));
        }
    }

    @Nested
    @DisplayName("응답 형식 테스트")
    class ResponseFormatTests {

        @Test
        @DisplayName("단어 응답에 필수 필드가 모두 포함됨")
        void response_containsAllRequiredFields() throws Exception {
            // given
            WordDto wordDto = createMockWordDto(1L, "사과");
            when(wordService.findById(1L)).thenReturn(Optional.of(wordDto));

            // when & then
            mockMvc.perform(get("/api/words/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.word").exists())
                    .andExpect(jsonPath("$.length").exists())
                    .andExpect(jsonPath("$.definitions").exists());
        }

        @Test
        @DisplayName("정의 응답에 필수 필드가 모두 포함됨")
        void definitionResponse_containsAllRequiredFields() throws Exception {
            // given
            WordDto wordDto = createMockWordDto(1L, "사과");
            when(wordService.findById(1L)).thenReturn(Optional.of(wordDto));

            // when & then
            mockMvc.perform(get("/api/words/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.definitions[0].senseOrder").exists())
                    .andExpect(jsonPath("$.definitions[0].definition").exists());
        }
    }
}

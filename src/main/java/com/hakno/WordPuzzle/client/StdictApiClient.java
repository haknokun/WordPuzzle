package com.hakno.WordPuzzle.client;

import com.hakno.WordPuzzle.client.dto.StdictSearchResponse;
import com.hakno.WordPuzzle.client.dto.StdictViewResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@Component
public class StdictApiClient {

    private final RestTemplate restTemplate;

    @Value("${stdict.api.key}")
    private String apiKey;

    @Value("${stdict.api.search-url}")
    private String searchUrl;

    @Value("${stdict.api.view-url}")
    private String viewUrl;

    public StdictApiClient() {
        this.restTemplate = new RestTemplate();

        // API returns text/json instead of application/json
        // Configure Jackson converter to handle text/json
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                new MediaType("text", "json", StandardCharsets.UTF_8)
        ));
        this.restTemplate.getMessageConverters().add(0, converter);
    }

    /**
     * 단어 검색 API
     *
     * @param query 검색어
     * @param start 시작 번호 (1~1000)
     * @param num   결과 수 (10~100)
     * @return 검색 결과
     */
    public StdictSearchResponse search(String query, int start, int num) {
        try {
            // API requires num between 10-100
            int validNum = Math.max(10, Math.min(num, 100));
            URI uri = UriComponentsBuilder.fromUriString(searchUrl)
                    .queryParam("key", apiKey)
                    .queryParam("q", query)
                    .queryParam("req_type", "json")
                    .queryParam("start", start)
                    .queryParam("num", validNum)
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();

            log.debug("Calling stdict search API: {}", uri);
            return restTemplate.getForObject(uri, StdictSearchResponse.class);
        } catch (RestClientException e) {
            log.error("Failed to call stdict search API: {}", e.getMessage());
            throw new RuntimeException("표준국어대사전 검색 API 호출 실패", e);
        }
    }

    /**
     * 단어 상세 조회 API
     *
     * @param targetCode 단어 고유 코드
     * @return 상세 정보
     */
    public StdictViewResponse getWordDetail(String targetCode) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(viewUrl)
                    .queryParam("key", apiKey)
                    .queryParam("target_code", targetCode)
                    .queryParam("req_type", "json")
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();

            log.debug("Calling stdict view API: {}", uri);
            return restTemplate.getForObject(uri, StdictViewResponse.class);
        } catch (RestClientException e) {
            log.error("Failed to call stdict view API for {}: {}", targetCode, e.getMessage());
            throw new RuntimeException("표준국어대사전 상세 API 호출 실패", e);
        }
    }

    /**
     * 첫 글자로 시작하는 단어 검색 (start 방식)
     *
     * @param firstChar 첫 글자
     * @param start     시작 번호
     * @param num       결과 수
     * @return 검색 결과
     */
    public StdictSearchResponse searchByFirstChar(String firstChar, int start, int num) {
        return searchAdvanced(firstChar, start, num, "start");
    }

    /**
     * 고급 검색 API
     *
     * @param query  검색어
     * @param start  시작 번호
     * @param num    결과 수
     * @param method 검색 방식 (exact, include, start, end, wildcard)
     * @return 검색 결과
     */
    public StdictSearchResponse searchAdvanced(String query, int start, int num, String method) {
        try {
            // API requires num between 10-100
            int validNum = Math.max(10, Math.min(num, 100));
            URI uri = UriComponentsBuilder.fromUriString(searchUrl)
                    .queryParam("key", apiKey)
                    .queryParam("q", query)
                    .queryParam("req_type", "json")
                    .queryParam("start", start)
                    .queryParam("num", validNum)
                    .queryParam("advanced", "y")
                    .queryParam("method", method)
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();

            log.debug("Calling stdict advanced search API: {}", uri);
            return restTemplate.getForObject(uri, StdictSearchResponse.class);
        } catch (RestClientException e) {
            log.error("Failed to call stdict advanced search API: {}", e.getMessage());
            throw new RuntimeException("표준국어대사전 고급 검색 API 호출 실패", e);
        }
    }

    /**
     * API 연결 테스트
     *
     * @return 연결 성공 여부
     */
    public boolean testConnection() {
        try {
            // Use simple query "가" with minimum num=10
            StdictSearchResponse response = search("가", 1, 10);
            return response != null && response.getChannel() != null;
        } catch (Exception e) {
            log.error("API connection test failed: {}", e.getMessage());
            return false;
        }
    }
}

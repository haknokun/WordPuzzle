package com.hakno.WordPuzzle.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StdictSearchResponse {

    @JsonProperty("channel")
    private Channel channel;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {
        private String title;
        private String link;
        private String description;
        @JsonProperty("lastbuilddate")
        private String lastBuildDate;
        private int total;
        private int start;
        private int num;
        private List<Item> item;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("target_code")
        private String targetCode;  // 표준국어대사전 고유 코드

        @JsonProperty("word")
        private String word;  // 표제어

        @JsonProperty("sup_no")
        private String supNo;  // 어깨번호 (String으로 수정)

        @JsonProperty("pos")
        private String pos;  // 품사

        @JsonProperty("origin")
        private String origin;  // 어원

        @JsonProperty("sense")
        private Sense sense;  // 검색 API에서는 단일 객체로 반환
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sense {
        @JsonProperty("sense_no")
        private String senseNo;  // 의미 번호

        @JsonProperty("definition")
        private String definition;  // 뜻풀이

        @JsonProperty("type")
        private String type;  // 유형

        @JsonProperty("cat")
        private String cat;  // 전문 분야

        @JsonProperty("link")
        private String link;  // 상세 링크
    }
}

package com.hakno.WordPuzzle.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StdictViewResponse {

    @JsonProperty("channel")
    private Channel channel;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {
        private String title;
        private String link;
        private String description;
        private String lastBuildDate;
        private int total;
        private List<Item> item;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("target_code")
        private String targetCode;

        @JsonProperty("word_info")
        private WordInfo wordInfo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WordInfo {
        @JsonProperty("word")
        private String word;

        @JsonProperty("word_unit")
        private String wordUnit;  // 단어 유형

        @JsonProperty("word_type")
        private String wordType;  // 고유어/한자어/외래어/혼종어

        @JsonProperty("original_language_info")
        private List<OriginalLanguageInfo> originalLanguageInfo;  // 어원 정보

        @JsonProperty("pronunciation_info")
        private List<PronunciationInfo> pronunciationInfo;  // 발음 정보

        @JsonProperty("pos_info")
        private List<PosInfo> posInfo;  // 품사 정보

        @JsonProperty("allomorph")
        private String allomorph;  // 이형태

        @JsonProperty("sup_no")
        private Integer supNo;  // 어깨번호
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OriginalLanguageInfo {
        @JsonProperty("original_language")
        private String originalLanguage;  // 어원 언어 (한자, 영어 등)

        @JsonProperty("language_type")
        private String languageType;  // 언어 유형
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PronunciationInfo {
        @JsonProperty("pronunciation")
        private String pronunciation;  // 발음
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PosInfo {
        @JsonProperty("pos")
        private String pos;  // 품사

        @JsonProperty("comm_pattern_info")
        private List<CommPatternInfo> commPatternInfo;  // 문형 정보
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommPatternInfo {
        @JsonProperty("sense_info")
        private List<SenseInfo> senseInfo;  // 의미 정보
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SenseInfo {
        @JsonProperty("sense_no")
        private String senseNo;  // 의미 번호

        @JsonProperty("definition")
        private String definition;  // 뜻풀이

        @JsonProperty("type")
        private String type;  // 유형

        @JsonProperty("cat_info")
        private List<CatInfo> catInfo;  // 분야 정보

        @JsonProperty("example_info")
        private List<ExampleInfo> exampleInfo;  // 용례

        @JsonProperty("relation_info")
        private List<RelationInfo> relationInfo;  // 어휘 관계
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatInfo {
        @JsonProperty("cat")
        private String cat;  // 전문 분야
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExampleInfo {
        @JsonProperty("example")
        private String example;  // 용례

        @JsonProperty("source")
        private String source;  // 출전

        @JsonProperty("translation")
        private String translation;  // 번역

        @JsonProperty("origin")
        private String origin;  // 원문
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RelationInfo {
        @JsonProperty("type")
        private String type;  // 관계 유형 (비슷한말, 반대말 등)

        @JsonProperty("word")
        private String word;  // 관련 단어

        @JsonProperty("link_target_code")
        private String linkTargetCode;  // 관련 단어 코드

        @JsonProperty("link")
        private String link;  // 링크
    }
}

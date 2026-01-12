package com.hakno.WordPuzzle.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hakno.WordPuzzle.entity.Definition;
import com.hakno.WordPuzzle.entity.Word;
import com.hakno.WordPuzzle.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @deprecated 한국어기초사전 JSON 파일 기반 레거시 임포트 서비스.
 * 표준국어대사전 API 기반의 {@link StdictImportService}를 사용하세요.
 * Phase E에서 완전히 제거될 예정입니다.
 */
@Deprecated(since = "2.0.0", forRemoval = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class DataImportService {

    private final WordRepository wordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @CacheEvict(value = {"wordsContaining", "wordsByPosition", "randomWords"}, allEntries = true)
    public int importFromDirectory(String directoryPath) throws IOException {
        int totalImported = 0;

        try (Stream<Path> paths = Files.list(Path.of(directoryPath))) {
            List<Path> jsonFiles = paths
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();

            for (Path jsonFile : jsonFiles) {
                log.info("Importing file: {}", jsonFile.getFileName());
                int count = importFromFile(jsonFile.toFile());
                totalImported += count;
                log.info("Imported {} words from {}", count, jsonFile.getFileName());
            }
        }

        return totalImported;
    }

    @Transactional
    public int importFromFile(File file) throws IOException {
        JsonNode root = objectMapper.readTree(file);
        JsonNode lexicalEntries = root
                .path("LexicalResource")
                .path("Lexicon")
                .path("LexicalEntry");

        if (!lexicalEntries.isArray()) {
            log.warn("No LexicalEntry array found in file: {}", file.getName());
            return 0;
        }

        List<Word> wordsToSave = new ArrayList<>();

        for (JsonNode entry : lexicalEntries) {
            try {
                Word word = parseEntry(entry);
                if (word != null && !wordRepository.existsByWord(word.getWord())) {
                    wordsToSave.add(word);
                }
            } catch (Exception e) {
                log.warn("Failed to parse entry: {}", e.getMessage());
            }
        }

        wordRepository.saveAll(wordsToSave);
        return wordsToSave.size();
    }

    private Word parseEntry(JsonNode entry) {
        // 단어 추출
        JsonNode lemmaFeat = entry.path("Lemma").path("feat");
        String wordText = extractValue(lemmaFeat, "writtenForm");

        if (wordText == null || wordText.isBlank()) {
            return null;
        }

        // 한글만 포함된 단어인지 확인 (십자말풀이용)
        if (!wordText.matches("^[가-힣]+$")) {
            return null;
        }

        // 품사 및 난이도 추출
        JsonNode entryFeat = entry.path("feat");
        String partOfSpeech = extractValue(entryFeat, "partOfSpeech");
        String vocabularyLevel = extractValue(entryFeat, "vocabularyLevel");

        // "없음" 또는 "품사 없음" 처리
        if ("없음".equals(vocabularyLevel)) {
            vocabularyLevel = null;
        }
        if ("품사 없음".equals(partOfSpeech)) {
            partOfSpeech = null;
        }

        Word word = Word.builder()
                .word(wordText)
                .partOfSpeech(partOfSpeech)
                .vocabularyLevel(vocabularyLevel)
                .build();

        // 뜻풀이 추출
        JsonNode senses = entry.path("Sense");
        if (senses.isArray()) {
            int order = 1;
            for (JsonNode sense : senses) {
                String definition = extractDefinition(sense);
                if (definition != null && !definition.isBlank()) {
                    Definition def = Definition.builder()
                            .senseOrder(order++)
                            .definition(definition)
                            .build();
                    word.addDefinition(def);
                }
            }
        }

        // 뜻풀이가 없으면 저장하지 않음
        if (word.getDefinitions().isEmpty()) {
            return null;
        }

        return word;
    }

    private String extractDefinition(JsonNode sense) {
        // feat이 객체인 경우 (단일 정의)
        JsonNode feat = sense.path("feat");
        if (feat.isObject()) {
            String att = feat.path("att").asText();
            if ("definition".equals(att)) {
                return feat.path("val").asText();
            }
        }
        // feat이 배열인 경우
        if (feat.isArray()) {
            for (JsonNode f : feat) {
                if ("definition".equals(f.path("att").asText())) {
                    return f.path("val").asText();
                }
            }
        }
        return null;
    }

    private String extractValue(JsonNode feat, String attribute) {
        if (feat.isObject()) {
            if (attribute.equals(feat.path("att").asText())) {
                return feat.path("val").asText();
            }
        }
        if (feat.isArray()) {
            for (JsonNode f : feat) {
                if (attribute.equals(f.path("att").asText())) {
                    return f.path("val").asText();
                }
            }
        }
        return null;
    }
}

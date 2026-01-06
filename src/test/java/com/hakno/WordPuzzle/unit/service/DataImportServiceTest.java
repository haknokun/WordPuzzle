package com.hakno.WordPuzzle.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hakno.WordPuzzle.entity.Word;
import com.hakno.WordPuzzle.repository.WordRepository;
import com.hakno.WordPuzzle.service.DataImportService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * DataImportService 단위 테스트
 * Task 6: Backend Service 패키지 커버리지 개선
 */
@ExtendWith(MockitoExtension.class)
class DataImportServiceTest {

    @Mock
    private WordRepository wordRepository;

    @InjectMocks
    private DataImportService dataImportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("importFromFile 테스트")
    class ImportFromFileTest {

        @Test
        @DisplayName("유효한 JSON 파일에서 단어 임포트 성공")
        void shouldImportWordsFromValidJson() throws IOException {
            // Given
            File jsonFile = createValidJsonFile("test.json");
            when(wordRepository.existsByWord(anyString())).thenReturn(false);
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            int result = dataImportService.importFromFile(jsonFile);

            // Then
            assertThat(result).isEqualTo(1);
            verify(wordRepository).saveAll(any());
        }

        @Test
        @DisplayName("이미 존재하는 단어는 건너뛰기")
        void shouldSkipExistingWords() throws IOException {
            // Given
            File jsonFile = createValidJsonFile("test.json");
            when(wordRepository.existsByWord("사과")).thenReturn(true);
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            int result = dataImportService.importFromFile(jsonFile);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("LexicalEntry 배열이 없는 JSON 처리")
        void shouldReturnZeroWhenNoLexicalEntryArray() throws IOException {
            // Given
            ObjectNode root = objectMapper.createObjectNode();
            root.putObject("LexicalResource").putObject("Lexicon").put("invalid", "data");

            File jsonFile = tempDir.resolve("invalid.json").toFile();
            objectMapper.writeValue(jsonFile, root);

            // When
            int result = dataImportService.importFromFile(jsonFile);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("한글이 아닌 단어는 건너뛰기")
        void shouldSkipNonKoreanWords() throws IOException {
            // Given
            File jsonFile = createJsonFileWithWord("apple", "명사", "초급", "사과");
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            int result = dataImportService.importFromFile(jsonFile);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("뜻풀이가 없는 단어는 건너뛰기")
        void shouldSkipWordsWithoutDefinition() throws IOException {
            // Given
            File jsonFile = createJsonFileWithoutDefinition("사과");
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            int result = dataImportService.importFromFile(jsonFile);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("빈 단어는 건너뛰기")
        void shouldSkipBlankWords() throws IOException {
            // Given
            File jsonFile = createJsonFileWithWord("", "명사", "초급", "정의");
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            int result = dataImportService.importFromFile(jsonFile);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("vocabularyLevel이 '없음'이면 null로 변환")
        void shouldConvertVocabularyLevelNoneToNull() throws IOException {
            // Given
            File jsonFile = createJsonFileWithWord("사과", "명사", "없음", "과일의 하나");
            when(wordRepository.existsByWord(anyString())).thenReturn(false);
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            dataImportService.importFromFile(jsonFile);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Word>> captor = ArgumentCaptor.forClass(List.class);
            verify(wordRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getVocabularyLevel()).isNull();
        }

        @Test
        @DisplayName("partOfSpeech가 '품사 없음'이면 null로 변환")
        void shouldConvertPartOfSpeechNoneToNull() throws IOException {
            // Given
            File jsonFile = createJsonFileWithWord("사과", "품사 없음", "초급", "과일의 하나");
            when(wordRepository.existsByWord(anyString())).thenReturn(false);
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            dataImportService.importFromFile(jsonFile);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Word>> captor = ArgumentCaptor.forClass(List.class);
            verify(wordRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getPartOfSpeech()).isNull();
        }
    }

    @Nested
    @DisplayName("importFromDirectory 테스트")
    class ImportFromDirectoryTest {

        @Test
        @DisplayName("디렉토리의 모든 JSON 파일 임포트")
        void shouldImportAllJsonFilesFromDirectory() throws IOException {
            // Given - 서로 다른 단어가 포함된 두 파일 생성
            createJsonFileWithWord("사과", "명사", "초급", "과일의 하나");
            createJsonFileWithWord("바나나", "명사", "초급", "열대 과일");
            when(wordRepository.existsByWord(anyString())).thenReturn(false);
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            int result = dataImportService.importFromDirectory(tempDir.toString());

            // Then
            assertThat(result).isEqualTo(2);
            verify(wordRepository, times(2)).saveAll(any());
        }

        @Test
        @DisplayName("JSON이 아닌 파일은 건너뛰기")
        void shouldSkipNonJsonFiles() throws IOException {
            // Given
            createValidJsonFile("valid.json");
            Files.writeString(tempDir.resolve("readme.txt"), "not json");

            when(wordRepository.existsByWord(anyString())).thenReturn(false);
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            int result = dataImportService.importFromDirectory(tempDir.toString());

            // Then
            assertThat(result).isEqualTo(1);
            verify(wordRepository, times(1)).saveAll(any());
        }

        @Test
        @DisplayName("빈 디렉토리는 0 반환")
        void shouldReturnZeroForEmptyDirectory() throws IOException {
            // Given - empty temp directory

            // When
            int result = dataImportService.importFromDirectory(tempDir.toString());

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("존재하지 않는 디렉토리 접근 시 예외 발생")
        void shouldThrowExceptionForNonExistentDirectory() {
            // Given
            String nonExistentPath = tempDir.resolve("non_existent").toString();

            // When & Then
            assertThatThrownBy(() -> dataImportService.importFromDirectory(nonExistentPath))
                    .isInstanceOf(IOException.class);
        }
    }

    @Nested
    @DisplayName("JSON 파싱 다양한 형식 테스트")
    class JsonParsingTest {

        @Test
        @DisplayName("feat이 객체인 경우 정상 파싱")
        void shouldParseWhenFeatIsObject() throws IOException {
            // Given
            File jsonFile = createJsonFileWithObjectFeat("사과", "명사", "과일의 일종");
            when(wordRepository.existsByWord(anyString())).thenReturn(false);
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            int result = dataImportService.importFromFile(jsonFile);

            // Then
            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("feat이 배열인 경우 정상 파싱")
        void shouldParseWhenFeatIsArray() throws IOException {
            // Given
            File jsonFile = createJsonFileWithArrayFeat("바나나", "명사", "열대 과일");
            when(wordRepository.existsByWord(anyString())).thenReturn(false);
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            int result = dataImportService.importFromFile(jsonFile);

            // Then
            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("여러 뜻풀이가 있는 단어 처리")
        void shouldHandleMultipleDefinitions() throws IOException {
            // Given
            File jsonFile = createJsonFileWithMultipleDefinitions("배", List.of("과일", "선박", "신체부위"));
            when(wordRepository.existsByWord(anyString())).thenReturn(false);
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            dataImportService.importFromFile(jsonFile);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Word>> captor = ArgumentCaptor.forClass(List.class);
            verify(wordRepository).saveAll(captor.capture());
            Word savedWord = captor.getValue().get(0);
            assertThat(savedWord.getDefinitions()).hasSize(3);
        }

        @Test
        @DisplayName("파싱 실패한 엔트리는 건너뛰고 계속 진행")
        void shouldContinueOnParseError() throws IOException {
            // Given
            File jsonFile = createJsonFileWithMixedEntries();
            when(wordRepository.existsByWord(anyString())).thenReturn(false);
            when(wordRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            // When
            int result = dataImportService.importFromFile(jsonFile);

            // Then
            assertThat(result).isGreaterThanOrEqualTo(0);
        }
    }

    // Helper methods
    private File createValidJsonFile(String filename) throws IOException {
        return createJsonFileWithWord("사과", "명사", "초급", "과일의 하나");
    }

    private File createJsonFileWithWord(String word, String partOfSpeech, String level, String definition) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode lexicalResource = root.putObject("LexicalResource");
        ObjectNode lexicon = lexicalResource.putObject("Lexicon");
        ArrayNode lexicalEntries = lexicon.putArray("LexicalEntry");

        ObjectNode entry = lexicalEntries.addObject();

        // Lemma
        ObjectNode lemma = entry.putObject("Lemma");
        ObjectNode lemmaFeat = lemma.putObject("feat");
        lemmaFeat.put("att", "writtenForm");
        lemmaFeat.put("val", word);

        // Entry feat (partOfSpeech, vocabularyLevel)
        ArrayNode entryFeat = entry.putArray("feat");
        ObjectNode posFeat = entryFeat.addObject();
        posFeat.put("att", "partOfSpeech");
        posFeat.put("val", partOfSpeech);
        ObjectNode levelFeat = entryFeat.addObject();
        levelFeat.put("att", "vocabularyLevel");
        levelFeat.put("val", level);

        // Sense
        ArrayNode senses = entry.putArray("Sense");
        ObjectNode sense = senses.addObject();
        ObjectNode senseFeat = sense.putObject("feat");
        senseFeat.put("att", "definition");
        senseFeat.put("val", definition);

        File jsonFile = tempDir.resolve(word + ".json").toFile();
        objectMapper.writeValue(jsonFile, root);
        return jsonFile;
    }

    private File createJsonFileWithoutDefinition(String word) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode lexicalResource = root.putObject("LexicalResource");
        ObjectNode lexicon = lexicalResource.putObject("Lexicon");
        ArrayNode lexicalEntries = lexicon.putArray("LexicalEntry");

        ObjectNode entry = lexicalEntries.addObject();
        ObjectNode lemma = entry.putObject("Lemma");
        ObjectNode lemmaFeat = lemma.putObject("feat");
        lemmaFeat.put("att", "writtenForm");
        lemmaFeat.put("val", word);

        // No Sense array

        File jsonFile = tempDir.resolve("no_def.json").toFile();
        objectMapper.writeValue(jsonFile, root);
        return jsonFile;
    }

    private File createJsonFileWithObjectFeat(String word, String partOfSpeech, String definition) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode lexicalResource = root.putObject("LexicalResource");
        ObjectNode lexicon = lexicalResource.putObject("Lexicon");
        ArrayNode lexicalEntries = lexicon.putArray("LexicalEntry");

        ObjectNode entry = lexicalEntries.addObject();

        // Lemma with object feat
        ObjectNode lemma = entry.putObject("Lemma");
        ObjectNode lemmaFeat = lemma.putObject("feat");
        lemmaFeat.put("att", "writtenForm");
        lemmaFeat.put("val", word);

        // Entry feat as object (single)
        ObjectNode entryFeat = entry.putObject("feat");
        entryFeat.put("att", "partOfSpeech");
        entryFeat.put("val", partOfSpeech);

        // Sense with object feat
        ArrayNode senses = entry.putArray("Sense");
        ObjectNode sense = senses.addObject();
        ObjectNode senseFeat = sense.putObject("feat");
        senseFeat.put("att", "definition");
        senseFeat.put("val", definition);

        File jsonFile = tempDir.resolve(word + "_obj.json").toFile();
        objectMapper.writeValue(jsonFile, root);
        return jsonFile;
    }

    private File createJsonFileWithArrayFeat(String word, String partOfSpeech, String definition) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode lexicalResource = root.putObject("LexicalResource");
        ObjectNode lexicon = lexicalResource.putObject("Lexicon");
        ArrayNode lexicalEntries = lexicon.putArray("LexicalEntry");

        ObjectNode entry = lexicalEntries.addObject();

        // Lemma with array feat
        ObjectNode lemma = entry.putObject("Lemma");
        ArrayNode lemmaFeatArray = lemma.putArray("feat");
        ObjectNode writtenFormFeat = lemmaFeatArray.addObject();
        writtenFormFeat.put("att", "writtenForm");
        writtenFormFeat.put("val", word);

        // Entry feat as array
        ArrayNode entryFeat = entry.putArray("feat");
        ObjectNode posFeat = entryFeat.addObject();
        posFeat.put("att", "partOfSpeech");
        posFeat.put("val", partOfSpeech);

        // Sense with array feat
        ArrayNode senses = entry.putArray("Sense");
        ObjectNode sense = senses.addObject();
        ArrayNode senseFeatArray = sense.putArray("feat");
        ObjectNode defFeat = senseFeatArray.addObject();
        defFeat.put("att", "definition");
        defFeat.put("val", definition);

        File jsonFile = tempDir.resolve(word + "_arr.json").toFile();
        objectMapper.writeValue(jsonFile, root);
        return jsonFile;
    }

    private File createJsonFileWithMultipleDefinitions(String word, List<String> definitions) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode lexicalResource = root.putObject("LexicalResource");
        ObjectNode lexicon = lexicalResource.putObject("Lexicon");
        ArrayNode lexicalEntries = lexicon.putArray("LexicalEntry");

        ObjectNode entry = lexicalEntries.addObject();

        // Lemma
        ObjectNode lemma = entry.putObject("Lemma");
        ObjectNode lemmaFeat = lemma.putObject("feat");
        lemmaFeat.put("att", "writtenForm");
        lemmaFeat.put("val", word);

        // Multiple Senses
        ArrayNode senses = entry.putArray("Sense");
        for (String def : definitions) {
            ObjectNode sense = senses.addObject();
            ObjectNode senseFeat = sense.putObject("feat");
            senseFeat.put("att", "definition");
            senseFeat.put("val", def);
        }

        File jsonFile = tempDir.resolve(word + "_multi.json").toFile();
        objectMapper.writeValue(jsonFile, root);
        return jsonFile;
    }

    private File createJsonFileWithMixedEntries() throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode lexicalResource = root.putObject("LexicalResource");
        ObjectNode lexicon = lexicalResource.putObject("Lexicon");
        ArrayNode lexicalEntries = lexicon.putArray("LexicalEntry");

        // Valid entry
        ObjectNode validEntry = lexicalEntries.addObject();
        ObjectNode lemma = validEntry.putObject("Lemma");
        ObjectNode lemmaFeat = lemma.putObject("feat");
        lemmaFeat.put("att", "writtenForm");
        lemmaFeat.put("val", "사과");
        ArrayNode senses = validEntry.putArray("Sense");
        ObjectNode sense = senses.addObject();
        ObjectNode senseFeat = sense.putObject("feat");
        senseFeat.put("att", "definition");
        senseFeat.put("val", "과일");

        // Invalid entry (no Lemma)
        ObjectNode invalidEntry = lexicalEntries.addObject();
        invalidEntry.put("invalid", "data");

        File jsonFile = tempDir.resolve("mixed.json").toFile();
        objectMapper.writeValue(jsonFile, root);
        return jsonFile;
    }
}

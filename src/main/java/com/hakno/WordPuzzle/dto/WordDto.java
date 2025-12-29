package com.hakno.WordPuzzle.dto;

import com.hakno.WordPuzzle.entity.Word;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WordDto {

    private Long id;
    private String word;
    private Integer length;
    private List<DefinitionDto> definitions;

    public static WordDto from(Word word) {
        return WordDto.builder()
                .id(word.getId())
                .word(word.getWord())
                .length(word.getLength())
                .definitions(word.getDefinitions().stream()
                        .map(DefinitionDto::from)
                        .toList())
                .build();
    }
}

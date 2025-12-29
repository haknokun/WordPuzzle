package com.hakno.WordPuzzle.dto;

import com.hakno.WordPuzzle.entity.Definition;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DefinitionDto {

    private Long id;
    private Integer senseOrder;
    private String definition;

    public static DefinitionDto from(Definition definition) {
        return DefinitionDto.builder()
                .id(definition.getId())
                .senseOrder(definition.getSenseOrder())
                .definition(definition.getDefinition())
                .build();
    }
}

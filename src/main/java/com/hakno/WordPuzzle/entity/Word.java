package com.hakno.WordPuzzle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "word", indexes = {
    @Index(name = "idx_word_length", columnList = "length"),
    @Index(name = "idx_word_word", columnList = "word"),
    @Index(name = "idx_word_first_char", columnList = "firstChar"),
    @Index(name = "idx_word_first_char_length", columnList = "firstChar, length")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private Integer length;

    @Column(nullable = false, length = 1)
    private String firstChar;

    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Definition> definitions = new ArrayList<>();

    @Builder
    public Word(String word) {
        this.word = word;
        this.length = word.length();
        this.firstChar = String.valueOf(word.charAt(0));
    }

    public void addDefinition(Definition definition) {
        definitions.add(definition);
        definition.setWord(this);
    }
}

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
    @Index(name = "idx_word_first_char_length", columnList = "firstChar, length"),
    @Index(name = "idx_word_level", columnList = "vocabularyLevel"),
    @Index(name = "idx_word_pos", columnList = "partOfSpeech")
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

    @Column(length = 20)
    private String partOfSpeech;  // 품사: 명사, 동사, 형용사 등

    @Column(length = 10)
    private String vocabularyLevel;  // 난이도: 초급, 중급, 고급

    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Definition> definitions = new ArrayList<>();

    @Builder
    public Word(String word, String partOfSpeech, String vocabularyLevel) {
        this.word = word;
        this.length = word.length();
        this.firstChar = String.valueOf(word.charAt(0));
        this.partOfSpeech = partOfSpeech;
        this.vocabularyLevel = vocabularyLevel;
    }

    public void addDefinition(Definition definition) {
        definitions.add(definition);
        definition.setWord(this);
    }
}

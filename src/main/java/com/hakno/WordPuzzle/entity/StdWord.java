package com.hakno.WordPuzzle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "std_word", indexes = {
    @Index(name = "idx_std_word_length", columnList = "length"),
    @Index(name = "idx_std_word_first_char", columnList = "firstChar"),
    @Index(name = "idx_std_word_first_char_length", columnList = "firstChar, length"),
    @Index(name = "idx_std_word_word_type", columnList = "wordType"),
    @Index(name = "idx_std_word_target_code", columnList = "targetCode", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StdWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String targetCode;  // 표준국어대사전 고유 코드

    @Column(nullable = false)
    private String word;  // 표제어

    @Column
    private Integer supNo;  // 어깨번호 (동음이의어 구분)

    @Column(nullable = false)
    private Integer length;  // 글자 수

    @Column(nullable = false, length = 1)
    private String firstChar;  // 첫 글자

    @Column(length = 20)
    private String wordType;  // 고유어/한자어/외래어/혼종어

    @Column(length = 500)
    private String origin;  // 어원 (한자 등)

    @Column(length = 100)
    private String pronunciation;  // 발음

    @Column(length = 200)
    private String allomorph;  // 이형태

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StdSense> senses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public StdWord(String targetCode, String word, Integer supNo, String wordType,
                   String origin, String pronunciation, String allomorph) {
        this.targetCode = targetCode;
        this.word = word;
        this.supNo = supNo;
        this.length = word != null ? word.length() : 0;
        this.firstChar = word != null && !word.isEmpty() ? String.valueOf(word.charAt(0)) : "";
        this.wordType = wordType;
        this.origin = origin;
        this.pronunciation = pronunciation;
        this.allomorph = allomorph;
    }

    public void addSense(StdSense sense) {
        senses.add(sense);
        sense.setWord(this);
    }
}

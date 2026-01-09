package com.hakno.WordPuzzle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "std_example")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StdExample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sense_id", nullable = false)
    private StdSense sense;

    @Column(columnDefinition = "TEXT")
    private String example;  // 용례 문장

    @Column(length = 200)
    private String source;  // 출전

    @Column(columnDefinition = "TEXT")
    private String translation;  // 번역 (한문 등)

    @Column(columnDefinition = "TEXT")
    private String origin;  // 원문

    @Builder
    public StdExample(String example, String source, String translation, String origin) {
        this.example = example;
        this.source = source;
        this.translation = translation;
        this.origin = origin;
    }
}

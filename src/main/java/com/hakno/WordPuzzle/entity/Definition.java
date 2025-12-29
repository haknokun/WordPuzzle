package com.hakno.WordPuzzle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Definition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;

    @Column(nullable = false)
    private Integer senseOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Builder
    public Definition(Integer senseOrder, String definition) {
        this.senseOrder = senseOrder;
        this.definition = definition;
    }
}

package com.hakno.WordPuzzle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @deprecated 한국어기초사전 기반 레거시 엔티티.
 * 표준국어대사전 기반의 {@link StdSense}를 사용하세요.
 * Phase E에서 완전히 제거될 예정입니다.
 */
@Deprecated(since = "2.0.0", forRemoval = true)
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

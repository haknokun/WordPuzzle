package com.hakno.WordPuzzle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "std_word_relation", indexes = {
    @Index(name = "idx_std_relation_type", columnList = "relationType")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StdWordRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sense_id", nullable = false)
    private StdSense sense;

    @Column(length = 20, nullable = false)
    private String relationType;  // 비슷한말/반대말/상위어/하위어/참고

    @Column(nullable = false)
    private String relatedWord;  // 관련 단어

    @Column(length = 20)
    private String relatedTargetCode;  // 관련 단어의 표준국어대사전 코드

    @Column(length = 500)
    private String link;  // 링크 URL

    @Builder
    public StdWordRelation(String relationType, String relatedWord,
                           String relatedTargetCode, String link) {
        this.relationType = relationType;
        this.relatedWord = relatedWord;
        this.relatedTargetCode = relatedTargetCode;
        this.link = link;
    }
}

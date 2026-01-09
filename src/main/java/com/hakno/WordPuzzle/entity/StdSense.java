package com.hakno.WordPuzzle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "std_sense", indexes = {
    @Index(name = "idx_std_sense_category", columnList = "category"),
    @Index(name = "idx_std_sense_pos", columnList = "pos")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StdSense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private StdWord word;

    @Column(length = 20)
    private String senseCode;  // 의미 고유 코드

    @Column
    private Integer senseOrder;  // 의미 순서

    @Column(length = 30)
    private String pos;  // 품사 (명사, 동사, 형용사 등)

    @Column(length = 50)
    private String category;  // 전문 분야 (의학, 법률 등)

    @Column(columnDefinition = "TEXT")
    private String definition;  // 뜻풀이

    @Column(length = 20)
    private String type;  // 일반어/전문어/방언 등

    @OneToMany(mappedBy = "sense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StdExample> examples = new ArrayList<>();

    @OneToMany(mappedBy = "sense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StdWordRelation> relations = new ArrayList<>();

    @Builder
    public StdSense(String senseCode, Integer senseOrder, String pos, String category,
                    String definition, String type) {
        this.senseCode = senseCode;
        this.senseOrder = senseOrder;
        this.pos = pos;
        this.category = category;
        this.definition = definition;
        this.type = type;
    }

    public void addExample(StdExample example) {
        examples.add(example);
        example.setSense(this);
    }

    public void addRelation(StdWordRelation relation) {
        relations.add(relation);
        relation.setSense(this);
    }
}

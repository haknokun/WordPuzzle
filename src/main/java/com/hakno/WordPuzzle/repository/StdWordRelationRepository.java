package com.hakno.WordPuzzle.repository;

import com.hakno.WordPuzzle.entity.StdWordRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StdWordRelationRepository extends JpaRepository<StdWordRelation, Long> {

    List<StdWordRelation> findBySenseId(Long senseId);

    List<StdWordRelation> findByRelationType(String relationType);

    // 유의어 조회
    @Query("SELECT r FROM StdWordRelation r WHERE r.sense.id = :senseId AND r.relationType = '비슷한말'")
    List<StdWordRelation> findSynonyms(@Param("senseId") Long senseId);

    // 반의어 조회
    @Query("SELECT r FROM StdWordRelation r WHERE r.sense.id = :senseId AND r.relationType = '반대말'")
    List<StdWordRelation> findAntonyms(@Param("senseId") Long senseId);

    // 상위어 조회
    @Query("SELECT r FROM StdWordRelation r WHERE r.sense.id = :senseId AND r.relationType = '상위어'")
    List<StdWordRelation> findHypernyms(@Param("senseId") Long senseId);

    // 하위어 조회
    @Query("SELECT r FROM StdWordRelation r WHERE r.sense.id = :senseId AND r.relationType = '하위어'")
    List<StdWordRelation> findHyponyms(@Param("senseId") Long senseId);

    // 단어 ID로 모든 관계 조회
    @Query("SELECT r FROM StdWordRelation r WHERE r.sense.word.id = :wordId")
    List<StdWordRelation> findByWordId(@Param("wordId") Long wordId);
}

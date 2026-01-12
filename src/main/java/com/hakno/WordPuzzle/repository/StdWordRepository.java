package com.hakno.WordPuzzle.repository;

import com.hakno.WordPuzzle.entity.StdWord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StdWordRepository extends JpaRepository<StdWord, Long> {

    // 기본 조회
    Optional<StdWord> findByTargetCode(String targetCode);

    boolean existsByTargetCode(String targetCode);

    // 길이 기반 조회
    List<StdWord> findByLengthBetween(int min, int max);

    long countByLengthBetween(int min, int max);

    // 첫 글자 + 길이 기반 조회
    List<StdWord> findByFirstCharAndLengthBetween(String firstChar, int min, int max);

    // 단어에 특정 글자 포함
    List<StdWord> findByWordContaining(String character);

    // 단어 유형별 조회
    List<StdWord> findByWordType(String wordType);

    // 랜덤 단어 조회 (퍼즐 생성용)
    @Query(value = "SELECT * FROM std_word WHERE length BETWEEN :min AND :max ORDER BY RAND() LIMIT :limit",
           nativeQuery = true)
    List<StdWord> findRandomWordsByLength(@Param("min") int min,
                                          @Param("max") int max,
                                          @Param("limit") int limit);

    // 특정 글자 포함 단어 랜덤 조회
    @Query(value = "SELECT * FROM std_word WHERE word LIKE %:char% AND length BETWEEN :min AND :max ORDER BY RAND() LIMIT :limit",
           nativeQuery = true)
    List<StdWord> findRandomWordsContainingChar(@Param("char") String character,
                                                 @Param("min") int min,
                                                 @Param("max") int max,
                                                 @Param("limit") int limit);

    // 분야별 조회 (JOIN 쿼리)
    @Query("SELECT DISTINCT w FROM StdWord w JOIN w.senses s WHERE s.category = :category")
    List<StdWord> findByCategory(@Param("category") String category);

    // 분야별 랜덤 조회
    @Query(value = "SELECT DISTINCT w.* FROM std_word w " +
                   "JOIN std_sense s ON w.id = s.word_id " +
                   "WHERE s.category = :category AND w.length BETWEEN :min AND :max " +
                   "ORDER BY RAND() LIMIT :limit",
           nativeQuery = true)
    List<StdWord> findRandomWordsByCategory(@Param("category") String category,
                                            @Param("min") int min,
                                            @Param("max") int max,
                                            @Param("limit") int limit);

    // 퍼즐 생성용 - 정의와 함께 랜덤 조회
    @Query("SELECT DISTINCT w FROM StdWord w LEFT JOIN FETCH w.senses WHERE w.length BETWEEN :min AND :max ORDER BY FUNCTION('RAND')")
    List<StdWord> findRandomWordsWithSenses(@Param("min") int min,
                                            @Param("max") int max,
                                            Pageable pageable);

    // 퍼즐 생성용 - 특정 글자 포함 단어 (정의 포함)
    @Query("SELECT DISTINCT w FROM StdWord w LEFT JOIN FETCH w.senses WHERE w.word LIKE %:char% AND w.length BETWEEN :min AND :max ORDER BY FUNCTION('RAND')")
    List<StdWord> findWordsContainingCharWithSenses(@Param("char") String character,
                                                     @Param("min") int min,
                                                     @Param("max") int max,
                                                     Pageable pageable);

    // 통계
    @Query("SELECT COUNT(w) FROM StdWord w")
    long countAll();

    @Query("SELECT w.wordType, COUNT(w) FROM StdWord w GROUP BY w.wordType")
    List<Object[]> countByWordType();

    @Query("SELECT w.length, COUNT(w) FROM StdWord w GROUP BY w.length ORDER BY w.length")
    List<Object[]> countByLength();

    // 카테고리 목록
    @Query("SELECT DISTINCT s.category FROM StdSense s WHERE s.category IS NOT NULL ORDER BY s.category")
    List<String> findAllCategories();

    // 단어유형별 랜덤 조회 (퍼즐 생성용)
    @Query(value = "SELECT * FROM std_word WHERE word_type = :wordType AND length BETWEEN :min AND :max ORDER BY RAND() LIMIT :limit",
           nativeQuery = true)
    List<StdWord> findRandomWordsByWordType(@Param("wordType") String wordType,
                                            @Param("min") int min,
                                            @Param("max") int max,
                                            @Param("limit") int limit);

    // 단어유형별 특정 글자 포함 단어 조회
    @Query(value = "SELECT * FROM std_word WHERE word_type = :wordType AND word LIKE %:char% AND length BETWEEN :min AND :max ORDER BY RAND() LIMIT :limit",
           nativeQuery = true)
    List<StdWord> findRandomWordsByWordTypeContainingChar(@Param("wordType") String wordType,
                                                          @Param("char") String character,
                                                          @Param("min") int min,
                                                          @Param("max") int max,
                                                          @Param("limit") int limit);

    // 분야별 특정 글자 포함 단어 조회
    @Query(value = "SELECT DISTINCT w.* FROM std_word w " +
                   "JOIN std_sense s ON w.id = s.word_id " +
                   "WHERE s.category = :category AND w.word LIKE %:char% AND w.length BETWEEN :min AND :max " +
                   "ORDER BY RAND() LIMIT :limit",
           nativeQuery = true)
    List<StdWord> findRandomWordsByCategoryContainingChar(@Param("category") String category,
                                                          @Param("char") String character,
                                                          @Param("min") int min,
                                                          @Param("max") int max,
                                                          @Param("limit") int limit);

    // 단어유형 목록 (DISTINCT)
    @Query("SELECT DISTINCT w.wordType FROM StdWord w WHERE w.wordType IS NOT NULL ORDER BY w.wordType")
    List<String> findAllWordTypes();
}

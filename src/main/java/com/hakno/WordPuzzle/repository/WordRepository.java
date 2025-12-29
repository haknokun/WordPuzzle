package com.hakno.WordPuzzle.repository;

import com.hakno.WordPuzzle.entity.Word;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    Optional<Word> findByWord(String word);

    List<Word> findByLength(Integer length);

    @Query("SELECT w FROM Word w WHERE w.length = :length ORDER BY FUNCTION('RAND')")
    List<Word> findRandomByLength(@Param("length") Integer length);

    @Query("SELECT w FROM Word w WHERE w.length BETWEEN :minLength AND :maxLength")
    List<Word> findByLengthBetween(@Param("minLength") Integer minLength, @Param("maxLength") Integer maxLength);

    boolean existsByWord(String word);

    // 랜덤 단어 검색 (Definition 함께 로드)
    @Query("SELECT DISTINCT w FROM Word w LEFT JOIN FETCH w.definitions " +
           "WHERE w.length BETWEEN :minLength AND :maxLength " +
           "ORDER BY FUNCTION('RAND')")
    List<Word> findRandomWordsWithDefinitions(
        @Param("minLength") Integer minLength,
        @Param("maxLength") Integer maxLength,
        Pageable pageable
    );

    // 특정 문자 포함 단어 검색 (LIKE)
    @Query("SELECT DISTINCT w FROM Word w LEFT JOIN FETCH w.definitions " +
           "WHERE w.word LIKE %:character% " +
           "AND w.length BETWEEN :minLength AND :maxLength " +
           "ORDER BY FUNCTION('RAND')")
    List<Word> findByContainingCharacterWithDefinitions(
        @Param("character") String character,
        @Param("minLength") Integer minLength,
        @Param("maxLength") Integer maxLength,
        Pageable pageable
    );

    // 특정 위치에 특정 글자가 있는 단어 검색
    @Query("SELECT DISTINCT w FROM Word w LEFT JOIN FETCH w.definitions " +
           "WHERE w.length = :length " +
           "AND SUBSTRING(w.word, :position, 1) = :character " +
           "ORDER BY FUNCTION('RAND')")
    List<Word> findByCharacterAtPositionWithDefinitions(
        @Param("character") String character,
        @Param("position") Integer position,
        @Param("length") Integer length,
        Pageable pageable
    );

    // 첫 글자로 검색
    @Query("SELECT DISTINCT w FROM Word w LEFT JOIN FETCH w.definitions " +
           "WHERE w.firstChar = :firstChar " +
           "AND w.length BETWEEN :minLength AND :maxLength " +
           "ORDER BY FUNCTION('RAND')")
    List<Word> findByFirstCharWithDefinitions(
        @Param("firstChar") String firstChar,
        @Param("minLength") Integer minLength,
        @Param("maxLength") Integer maxLength,
        Pageable pageable
    );
}

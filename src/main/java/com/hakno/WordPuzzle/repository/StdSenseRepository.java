package com.hakno.WordPuzzle.repository;

import com.hakno.WordPuzzle.entity.StdSense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StdSenseRepository extends JpaRepository<StdSense, Long> {

    List<StdSense> findByWordId(Long wordId);

    List<StdSense> findByCategory(String category);

    List<StdSense> findByPos(String pos);

    @Query("SELECT DISTINCT s.category FROM StdSense s WHERE s.category IS NOT NULL ORDER BY s.category")
    List<String> findAllCategories();

    @Query("SELECT s.category, COUNT(s) FROM StdSense s WHERE s.category IS NOT NULL GROUP BY s.category ORDER BY COUNT(s) DESC")
    List<Object[]> countByCategory();

    @Query("SELECT DISTINCT s.pos FROM StdSense s WHERE s.pos IS NOT NULL ORDER BY s.pos")
    List<String> findAllPos();
}

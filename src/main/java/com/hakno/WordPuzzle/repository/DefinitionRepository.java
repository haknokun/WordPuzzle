package com.hakno.WordPuzzle.repository;

import com.hakno.WordPuzzle.entity.Definition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefinitionRepository extends JpaRepository<Definition, Long> {

    List<Definition> findByWordId(Long wordId);
}

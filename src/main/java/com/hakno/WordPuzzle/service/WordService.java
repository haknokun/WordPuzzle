package com.hakno.WordPuzzle.service;

import com.hakno.WordPuzzle.dto.WordDto;
import com.hakno.WordPuzzle.entity.Word;
import com.hakno.WordPuzzle.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository wordRepository;

    public Optional<WordDto> findById(Long id) {
        return wordRepository.findById(id)
                .map(WordDto::from);
    }

    public Optional<WordDto> findByWord(String word) {
        return wordRepository.findByWord(word)
                .map(WordDto::from);
    }

    public List<WordDto> findByLength(Integer length) {
        return wordRepository.findByLength(length).stream()
                .map(WordDto::from)
                .toList();
    }

    public List<WordDto> findRandomByLength(Integer length, int limit) {
        return wordRepository.findRandomByLength(length).stream()
                .limit(limit)
                .map(WordDto::from)
                .toList();
    }

    public long count() {
        return wordRepository.count();
    }
}

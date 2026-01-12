package com.hakno.WordPuzzle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WordPuzzleApplication {

	public static void main(String[] args) {
		SpringApplication.run(WordPuzzleApplication.class, args);
	}

}

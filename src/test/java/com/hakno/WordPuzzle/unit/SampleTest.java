package com.hakno.WordPuzzle.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 테스트 환경 확인용 샘플 테스트
 * Phase 1: 테스트 프레임워크 정상 동작 확인
 */
class SampleTest {

    @Test
    @DisplayName("테스트 환경이 정상적으로 설정되었는지 확인")
    void testEnvironmentSetup() {
        // Given
        int a = 1;
        int b = 2;

        // When
        int result = a + b;

        // Then
        assertThat(result).isEqualTo(3);
    }

    @Test
    @DisplayName("AssertJ가 정상적으로 동작하는지 확인")
    void testAssertJWorks() {
        // Given
        String message = "Hello, TDD!";

        // Then
        assertThat(message)
            .isNotNull()
            .startsWith("Hello")
            .contains("TDD")
            .hasSize(11);
    }
}

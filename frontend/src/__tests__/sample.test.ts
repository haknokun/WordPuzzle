import { describe, it, expect } from 'vitest'

/**
 * 테스트 환경 확인용 샘플 테스트
 * Phase 1: Vitest 프레임워크 정상 동작 확인
 */
describe('테스트 환경 확인', () => {
  it('기본 산술 연산이 동작해야 한다', () => {
    // Given
    const a = 1
    const b = 2

    // When
    const result = a + b

    // Then
    expect(result).toBe(3)
  })

  it('문자열 검증이 동작해야 한다', () => {
    // Given
    const message = 'Hello, TDD!'

    // Then
    expect(message).toBeDefined()
    expect(message).toContain('TDD')
    expect(message).toHaveLength(11)
  })

  it('배열 검증이 동작해야 한다', () => {
    // Given
    const items = ['가', '나', '다']

    // Then
    expect(items).toHaveLength(3)
    expect(items).toContain('가')
    expect(items[0]).toBe('가')
  })

  it('객체 검증이 동작해야 한다', () => {
    // Given
    const cell = {
      row: 0,
      col: 1,
      letter: '가',
      isBlank: false,
    }

    // Then
    expect(cell).toMatchObject({
      row: 0,
      col: 1,
    })
    expect(cell.isBlank).toBe(false)
  })
})

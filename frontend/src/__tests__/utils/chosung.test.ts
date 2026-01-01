import { describe, it, expect } from 'vitest';
import { getChosung, CHOSUNG_LIST, isKoreanSyllable } from '../../utils/chosung';

describe('chosung 유틸리티', () => {
  describe('CHOSUNG_LIST', () => {
    it('19개의 초성을 포함해야 함', () => {
      expect(CHOSUNG_LIST).toHaveLength(19);
    });

    it('첫 번째 초성은 ㄱ이어야 함', () => {
      expect(CHOSUNG_LIST[0]).toBe('ㄱ');
    });

    it('마지막 초성은 ㅎ이어야 함', () => {
      expect(CHOSUNG_LIST[18]).toBe('ㅎ');
    });
  });

  describe('isKoreanSyllable', () => {
    it('한글 음절이면 true 반환', () => {
      expect(isKoreanSyllable('가')).toBe(true);
      expect(isKoreanSyllable('힣')).toBe(true);
      expect(isKoreanSyllable('한')).toBe(true);
    });

    it('한글이 아니면 false 반환', () => {
      expect(isKoreanSyllable('a')).toBe(false);
      expect(isKoreanSyllable('1')).toBe(false);
      expect(isKoreanSyllable('!')).toBe(false);
      expect(isKoreanSyllable('ㄱ')).toBe(false); // 자음만
      expect(isKoreanSyllable('ㅏ')).toBe(false); // 모음만
    });
  });

  describe('getChosung', () => {
    it('한글 단어의 초성을 추출해야 함', () => {
      expect(getChosung('한글')).toBe('ㅎㄱ');
      expect(getChosung('가나다')).toBe('ㄱㄴㄷ');
      expect(getChosung('사과')).toBe('ㅅㄱ');
    });

    it('쌍자음 초성도 정확히 추출해야 함', () => {
      expect(getChosung('까치')).toBe('ㄲㅊ');
      expect(getChosung('뚜껑')).toBe('ㄸㄲ');
      expect(getChosung('빨강')).toBe('ㅃㄱ');   // 강의 초성은 ㄱ
      expect(getChosung('쌍둥이')).toBe('ㅆㄷㅇ');
    });

    it('한글이 아닌 문자는 그대로 반환해야 함', () => {
      expect(getChosung('abc')).toBe('abc');
      expect(getChosung('123')).toBe('123');
      expect(getChosung('!@#')).toBe('!@#');
    });

    it('혼합된 문자열도 처리해야 함', () => {
      expect(getChosung('한1글')).toBe('ㅎ1ㄱ');
      expect(getChosung('A사과B')).toBe('AㅅㄱB');
      expect(getChosung('테스트123')).toBe('ㅌㅅㅌ123');
    });

    it('빈 문자열은 빈 문자열 반환', () => {
      expect(getChosung('')).toBe('');
    });

    it('공백 문자는 그대로 유지', () => {
      expect(getChosung('한 글')).toBe('ㅎ ㄱ');
      expect(getChosung('가 나 다')).toBe('ㄱ ㄴ ㄷ');
    });

    it('긴 단어도 처리해야 함', () => {
      expect(getChosung('프로그래밍')).toBe('ㅍㄹㄱㄹㅁ');
      expect(getChosung('알고리즘')).toBe('ㅇㄱㄹㅈ');
      expect(getChosung('컴퓨터과학')).toBe('ㅋㅍㅌㄱㅎ');
    });
  });
});

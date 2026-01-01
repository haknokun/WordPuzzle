/**
 * 한글 초성 관련 유틸리티 함수
 */

// 한글 초성 목록 (19개)
export const CHOSUNG_LIST = [
  'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
  'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
];

// 한글 유니코드 범위
const HANGUL_SYLLABLE_START = 0xAC00; // '가'
const HANGUL_SYLLABLE_END = 0xD7A3;   // '힣'
const SYLLABLE_PER_CHOSUNG = 588;     // 초성당 음절 수 (21 * 28)

/**
 * 문자가 한글 음절인지 확인
 * @param char 확인할 문자 (한 글자)
 * @returns 한글 음절이면 true
 */
export function isKoreanSyllable(char: string): boolean {
  if (char.length !== 1) return false;
  const code = char.charCodeAt(0);
  return code >= HANGUL_SYLLABLE_START && code <= HANGUL_SYLLABLE_END;
}

/**
 * 문자열에서 각 한글 글자의 초성을 추출
 * 한글이 아닌 문자는 그대로 반환
 * @param word 초성을 추출할 문자열
 * @returns 초성으로 변환된 문자열
 */
export function getChosung(word: string): string {
  return word.split('').map(char => {
    if (isKoreanSyllable(char)) {
      const code = char.charCodeAt(0);
      const chosungIndex = Math.floor((code - HANGUL_SYLLABLE_START) / SYLLABLE_PER_CHOSUNG);
      return CHOSUNG_LIST[chosungIndex];
    }
    return char;
  }).join('');
}

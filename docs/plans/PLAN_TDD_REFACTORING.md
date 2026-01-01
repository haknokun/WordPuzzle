# Implementation Plan: 십자말풀이 TDD 리팩토링

**Status**: 🔄 In Progress
**Started**: 2026-01-01
**Last Updated**: 2026-01-01

---

**CRITICAL INSTRUCTIONS**: After completing each phase:
1. Check off completed task checkboxes
2. Run all quality gate validation commands
3. Verify ALL quality gate items pass
4. Update "Last Updated" date above
5. Document learnings in Notes section
6. Only then proceed to next phase

**DO NOT skip quality gates or proceed with failing checks**

---

## Overview

### Feature Description
현재 테스트가 거의 없는 십자말풀이 프로젝트에 TDD 방식으로 테스트를 추가하고, 복잡한 로직을 테스트 가능한 작은 단위로 리팩토링합니다.

### Current State Analysis
| 영역 | 현재 상태 | 문제점 |
|------|----------|--------|
| Backend Tests | 컨텍스트 로드 테스트 1개 | 비즈니스 로직 테스트 없음 |
| Frontend Tests | 없음 | 테스트 환경 미구축 |
| PuzzleGeneratorService | 520줄 단일 클래스 | 테스트 불가능한 거대 메서드 |
| PuzzleGrid.tsx | 355줄 복잡한 컴포넌트 | 로직과 UI 혼재 |

### Success Criteria
- [ ] Backend 단위 테스트 커버리지 80% 이상
- [ ] Frontend 단위 테스트 커버리지 70% 이상
- [ ] 핵심 비즈니스 로직이 순수 함수로 분리됨
- [ ] E2E 테스트로 주요 사용자 플로우 검증
- [ ] 모든 테스트가 5분 내 실행 완료

### User Impact
- 코드 변경 시 회귀 버그 조기 발견
- 안정적인 리팩토링 가능
- 새 기능 추가 시 자신감 있는 개발

---

## Architecture Decisions

| Decision | Rationale | Trade-offs |
|----------|-----------|------------|
| PuzzleGeneratorService 로직을 순수 함수로 분리 | DB 의존 없이 단위 테스트 가능 | 클래스/파일 증가 |
| GridUtils 유틸리티 클래스 생성 | 그리드 관련 로직 재사용성 증가 | 초기 리팩토링 비용 |
| Frontend 커스텀 훅 분리 | 로직 테스트와 UI 테스트 분리 | 컴포넌트 구조 변경 필요 |
| Vitest 사용 (Frontend) | Vite 프로젝트와 통합 최적화 | Jest 대비 생태계 제한 |
| JUnit 5 + Mockito (Backend) | Spring Boot 기본 지원 | 없음 |

---

## Dependencies

### Required Before Starting
- [ ] Java 21 설치 확인
- [ ] Node.js 설치 확인
- [ ] MySQL 데이터베이스 실행 중

### External Dependencies (추가 예정)
**Backend**:
- JUnit 5 (기존)
- Mockito (기존)
- AssertJ (추가)

**Frontend**:
- Vitest (추가)
- @testing-library/react (추가)
- @testing-library/user-event (추가)
- jsdom (추가)

**E2E**:
- Playwright (추가)

---

## Test Strategy

### Testing Approach
**TDD Principle**: Write tests FIRST, then implement to make them pass

### Test Pyramid for This Feature
| Test Type | Coverage Target | Purpose |
|-----------|-----------------|---------|
| **Unit Tests** | >=80% (Backend), >=70% (Frontend) | 순수 함수, 유틸리티, 커스텀 훅 |
| **Integration Tests** | Critical paths | Controller-Service, React 컴포넌트 |
| **E2E Tests** | 2개 주요 플로우 | 퍼즐 생성, 퍼즐 풀이 완료 |

### Test File Organization
```
# Backend
src/test/java/com/hakno/WordPuzzle/
├── unit/
│   ├── service/
│   │   └── PuzzleGeneratorServiceTest.java
│   └── util/
│       └── GridUtilsTest.java
├── integration/
│   └── controller/
│       └── PuzzleControllerIntegrationTest.java
└── repository/
    └── WordRepositoryTest.java

# Frontend
frontend/src/
├── __tests__/
│   ├── utils/
│   │   ├── chosung.test.ts
│   │   └── puzzleUtils.test.ts
│   ├── hooks/
│   │   ├── usePuzzleNavigation.test.ts
│   │   └── usePuzzleCompletion.test.ts
│   └── components/
│       ├── PuzzleGrid.test.tsx
│       └── HintPanel.test.tsx
└── e2e/
    └── puzzle-flow.spec.ts
```

---

## Implementation Phases

### Phase 1: 테스트 환경 구축
**Goal**: Backend/Frontend 테스트 프레임워크 설정 및 첫 테스트 통과
**Status**: Complete

#### Tasks

**RED: Write Failing Tests First**
- [x] **Test 1.1**: Backend 샘플 테스트 작성
  - File: `src/test/java/com/hakno/WordPuzzle/unit/SampleTest.java`
  - Expected: 테스트 실행 환경 확인용 간단한 테스트
  - Details: assertEquals(1+1, 2) 수준의 환경 확인 테스트

- [x] **Test 1.2**: Frontend Vitest 설정 및 샘플 테스트
  - File: `frontend/src/__tests__/sample.test.ts`
  - Expected: Vitest가 올바르게 실행되는지 확인
  - Details: 기본 테스트 실행 확인

**GREEN: Implement to Make Tests Pass**
- [x] **Task 1.3**: Backend 테스트 의존성 확인 및 설정
  - File: `build.gradle`
  - Goal: JUnit 5, Mockito, AssertJ 의존성 확인
  - Details: 기존 의존성 활용, 필요시 AssertJ 추가

- [x] **Task 1.4**: Frontend Vitest 설치 및 설정
  - Files: `frontend/package.json`, `frontend/vite.config.ts`
  - Goal: Vitest + Testing Library 설치
  - Details:
    ```bash
    cd frontend
    npm install -D vitest @testing-library/react @testing-library/user-event jsdom @types/jest
    ```

- [x] **Task 1.5**: Playwright E2E 환경 설정
  - Files: `playwright.config.ts`, `package.json`
  - Goal: E2E 테스트 실행 환경 구축
  - Details: 기존 playwright.config.ts 확인 및 수정 (이미 68개 E2E 테스트 존재)

**REFACTOR: Clean Up Code**
- [x] **Task 1.6**: 테스트 스크립트 정리
  - Files: `package.json` (root & frontend)
  - Goal: npm test 명령어로 전체 테스트 실행 가능하게 설정

#### Quality Gate

**TDD Compliance**:
- [x] 테스트 환경 설정 완료
- [x] Backend: `./gradlew test` 성공 (3개 테스트 통과)
- [x] Frontend: `npm test` 성공 (4개 테스트 통과)
- [x] E2E: `npx playwright test --list` 성공 (68개 테스트 확인)

**Validation Commands**:
```bash
# Backend
./gradlew test

# Frontend
cd frontend && npm test

# E2E 환경 확인
npx playwright test --list
```

---

### Phase 2: Backend 순수 함수 추출 및 단위 테스트
**Goal**: PuzzleGeneratorService의 핵심 로직을 테스트 가능한 순수 함수로 분리
**Status**: Pending

#### Tasks

**RED: Write Failing Tests First**
- [ ] **Test 2.1**: GridUtils 테스트 작성
  - File: `src/test/java/com/hakno/WordPuzzle/unit/util/GridUtilsTest.java`
  - Expected: 테스트 실패 (GridUtils 클래스 없음)
  - Test Cases:
    - `calculateGridSize(5)` -> 11 (8 + 5*0.7)
    - `calculateGridSize(20)` -> 22 (8 + 20*0.7)
    - `calculateGridSize(30)` -> 25 (최대값 제한)
    - `countCommonChars("가나다")` -> 3
    - `countCommonChars("xyz")` -> 0

- [ ] **Test 2.2**: PlacementValidator 테스트 작성
  - File: `src/test/java/com/hakno/WordPuzzle/unit/service/PlacementValidatorTest.java`
  - Expected: 테스트 실패 (PlacementValidator 클래스 없음)
  - Test Cases:
    - 빈 그리드에 단어 배치 가능 여부
    - 범위 초과 시 배치 불가
    - 교차점 글자 불일치 시 배치 불가
    - 인접 규칙 위반 시 배치 불가
    - 정상 교차 배치 성공

- [ ] **Test 2.3**: GridConverter 테스트 작성
  - File: `src/test/java/com/hakno/WordPuzzle/unit/util/GridConverterTest.java`
  - Expected: 테스트 실패 (GridConverter 클래스 없음)
  - Test Cases:
    - char[][] -> List<List<PuzzleCell>> 변환
    - 번호 할당 정확성 검증
    - 빈 셀 isBlank=true 검증

**GREEN: Implement to Make Tests Pass**
- [ ] **Task 2.4**: GridUtils 클래스 생성
  - File: `src/main/java/com/hakno/WordPuzzle/util/GridUtils.java`
  - Goal: Test 2.1 통과
  - Details:
    - `calculateGridSize(int wordCount)` 추출
    - `countCommonChars(String word)` 추출
    - `COMMON_CHARS` 상수 이동

- [ ] **Task 2.5**: PlacementValidator 클래스 생성
  - File: `src/main/java/com/hakno/WordPuzzle/service/PlacementValidator.java`
  - Goal: Test 2.2 통과
  - Details:
    - `canPlaceWord(grid, word, row, col, direction, gridSize)` 추출
    - `whyCannotPlace(...)` 추출 (디버깅용)

- [ ] **Task 2.6**: GridConverter 클래스 생성
  - File: `src/main/java/com/hakno/WordPuzzle/util/GridConverter.java`
  - Goal: Test 2.3 통과
  - Details:
    - `convertToCellGrid(...)` 추출

- [ ] **Task 2.7**: PuzzleGeneratorService 리팩토링
  - File: `src/main/java/com/hakno/WordPuzzle/service/PuzzleGeneratorService.java`
  - Goal: 추출된 클래스들을 사용하도록 변경
  - Details: 기존 기능 유지하면서 의존성 주입

**REFACTOR: Clean Up Code**
- [ ] **Task 2.8**: 코드 정리
  - Files: 모든 신규 클래스
  - Checklist:
    - [ ] 중복 코드 제거
    - [ ] 명명 규칙 통일
    - [ ] JavaDoc 추가

#### Quality Gate

**TDD Compliance**:
- [ ] 테스트가 먼저 작성됨
- [ ] 모든 테스트 통과
- [ ] 커버리지 80% 이상 (신규 클래스)

**Validation Commands**:
```bash
# 테스트 실행
./gradlew test

# 커버리지 리포트 (JaCoCo 설정 필요)
./gradlew test jacocoTestReport

# 빌드 확인
./gradlew build
```

**Manual Test Checklist**:
- [ ] 퍼즐 생성 API 정상 동작 확인
- [ ] 기존 기능 회귀 없음 확인

---

### Phase 3: Backend 통합 테스트
**Goal**: Repository 및 Controller 통합 테스트 추가
**Status**: Pending

#### Tasks

**RED: Write Failing Tests First**
- [ ] **Test 3.1**: WordRepository 테스트 작성
  - File: `src/test/java/com/hakno/WordPuzzle/repository/WordRepositoryTest.java`
  - Expected: @DataJpaTest로 실제 DB 연동 테스트
  - Test Cases:
    - 난이도별 단어 검색
    - 특정 글자 포함 단어 검색
    - 결과 제한 (Pageable) 동작 확인

- [ ] **Test 3.2**: PuzzleController 통합 테스트 작성
  - File: `src/test/java/com/hakno/WordPuzzle/integration/PuzzleControllerIntegrationTest.java`
  - Expected: @SpringBootTest + MockMvc
  - Test Cases:
    - GET /api/puzzle/generate 정상 응답
    - 파라미터 검증 (gridSize, wordCount, level)
    - 에러 응답 형식

**GREEN: Implement to Make Tests Pass**
- [ ] **Task 3.3**: 테스트 데이터 준비
  - File: `src/test/resources/data.sql` 또는 @Sql 어노테이션
  - Goal: 테스트용 단어 데이터 삽입
  - Details: 최소 20개 테스트 단어 준비

- [ ] **Task 3.4**: Repository 테스트 통과
  - Goal: Test 3.1 통과
  - Details: 필요시 Repository 쿼리 수정

- [ ] **Task 3.5**: Controller 테스트 통과
  - Goal: Test 3.2 통과
  - Details: 응답 형식 검증, 예외 처리 확인

**REFACTOR: Clean Up Code**
- [ ] **Task 3.6**: 테스트 코드 정리
  - Files: 모든 테스트 파일
  - Checklist:
    - [ ] 테스트 데이터 팩토리 메서드 추출
    - [ ] 공통 설정 상속 구조화

#### Quality Gate

**TDD Compliance**:
- [ ] 통합 테스트 커버리지 확인
- [ ] 모든 테스트 통과

**Validation Commands**:
```bash
./gradlew test --tests "*IntegrationTest"
./gradlew test --tests "*RepositoryTest"
```

**Manual Test Checklist**:
- [ ] API 응답 시간 5초 이내
- [ ] 에러 시 적절한 HTTP 상태 코드 반환

---

### Phase 4: Frontend 유틸리티 함수 테스트
**Goal**: Frontend 순수 함수 추출 및 테스트
**Status**: Pending

#### Tasks

**RED: Write Failing Tests First**
- [ ] **Test 4.1**: chosung 유틸리티 테스트
  - File: `frontend/src/__tests__/utils/chosung.test.ts`
  - Expected: 테스트 실패 (함수 미추출 상태)
  - Test Cases:
    - `getChosung("한글")` -> "ㅎㄱ"
    - `getChosung("가나다")` -> "ㄱㄴㄷ"
    - `getChosung("abc")` -> "abc" (한글 아닌 경우)
    - `getChosung("한1글")` -> "ㅎ1ㄱ" (혼합)

- [ ] **Test 4.2**: puzzleUtils 테스트
  - File: `frontend/src/__tests__/utils/puzzleUtils.test.ts`
  - Expected: 테스트 실패 (함수 미추출 상태)
  - Test Cases:
    - `isCellInWord(cell, word)` - 셀이 단어에 속하는지
    - `checkCompletion(grid, userInputs)` - 완료 체크
    - `findWordAtCell(row, col, words)` - 셀 위치의 단어 찾기

**GREEN: Implement to Make Tests Pass**
- [ ] **Task 4.3**: chosung.ts 유틸리티 추출
  - File: `frontend/src/utils/chosung.ts`
  - Goal: Test 4.1 통과
  - Details: HintPanel.tsx에서 getChosung 함수 분리

- [ ] **Task 4.4**: puzzleUtils.ts 유틸리티 추출
  - File: `frontend/src/utils/puzzleUtils.ts`
  - Goal: Test 4.2 통과
  - Details:
    - PuzzleGrid.tsx에서 순수 함수 추출
    - `isCellInWord`, `checkCompletion`, `findWordAtCell`

- [ ] **Task 4.5**: 컴포넌트에서 유틸리티 사용
  - Files: `HintPanel.tsx`, `PuzzleGrid.tsx`
  - Goal: 추출된 유틸리티 import하여 사용
  - Details: 기존 동작 유지 확인

**REFACTOR: Clean Up Code**
- [ ] **Task 4.6**: 유틸리티 타입 정의 개선
  - Files: `types/puzzle.ts`, 유틸리티 파일들
  - Checklist:
    - [ ] 함수 시그니처 명확화
    - [ ] JSDoc 추가

#### Quality Gate

**TDD Compliance**:
- [ ] 유틸리티 함수 커버리지 100%
- [ ] 모든 테스트 통과

**Validation Commands**:
```bash
cd frontend
npm test -- --coverage
npm run lint
npm run build
```

**Manual Test Checklist**:
- [ ] 초성 힌트 정상 동작
- [ ] 퍼즐 완료 감지 정상 동작

---

### Phase 5: Frontend 커스텀 훅 및 컴포넌트 테스트
**Goal**: React 컴포넌트 로직을 커스텀 훅으로 분리하고 테스트
**Status**: Pending

#### Tasks

**RED: Write Failing Tests First**
- [ ] **Test 5.1**: usePuzzleNavigation 훅 테스트
  - File: `frontend/src/__tests__/hooks/usePuzzleNavigation.test.ts`
  - Expected: 훅 테스트 (renderHook 사용)
  - Test Cases:
    - 셀 클릭 시 selectedCell 업데이트
    - 화살표 키 이동
    - 단어 방향 전환

- [ ] **Test 5.2**: HintPanel 컴포넌트 테스트
  - File: `frontend/src/__tests__/components/HintPanel.test.tsx`
  - Expected: 렌더링 및 상호작용 테스트
  - Test Cases:
    - 가로/세로 힌트 렌더링
    - 힌트 클릭 시 onWordClick 호출
    - 초성 토글 동작

- [ ] **Test 5.3**: PuzzleGrid 컴포넌트 테스트
  - File: `frontend/src/__tests__/components/PuzzleGrid.test.tsx`
  - Expected: 렌더링 및 입력 테스트
  - Test Cases:
    - 그리드 렌더링
    - 셀 클릭 및 입력
    - 완료 시 onComplete 호출

**GREEN: Implement to Make Tests Pass**
- [ ] **Task 5.4**: usePuzzleNavigation 훅 추출
  - File: `frontend/src/hooks/usePuzzleNavigation.ts`
  - Goal: Test 5.1 통과
  - Details: 셀 선택, 키보드 네비게이션 로직 분리

- [ ] **Task 5.5**: usePuzzleInput 훅 추출
  - File: `frontend/src/hooks/usePuzzleInput.ts`
  - Goal: 입력 처리 로직 분리
  - Details: handleInput, handleComposition 로직

- [ ] **Task 5.6**: PuzzleGrid 리팩토링
  - File: `frontend/src/components/PuzzleGrid.tsx`
  - Goal: 추출된 훅 사용
  - Details: 컴포넌트 단순화, 테스트 통과

**REFACTOR: Clean Up Code**
- [ ] **Task 5.7**: 컴포넌트 코드 정리
  - Files: PuzzleGrid.tsx, 훅 파일들
  - Checklist:
    - [ ] 불필요한 의존성 제거
    - [ ] 타입 안전성 강화

#### Quality Gate

**TDD Compliance**:
- [ ] 훅 테스트 커버리지 80% 이상
- [ ] 컴포넌트 테스트 주요 경로 커버
- [ ] 모든 테스트 통과

**Validation Commands**:
```bash
cd frontend
npm test -- --coverage
npm run lint
npm run build
```

**Manual Test Checklist**:
- [ ] 키보드 네비게이션 정상 동작
- [ ] 한글 입력 정상 동작
- [ ] 셀 선택/하이라이팅 정상 동작

---

### Phase 6: E2E 테스트
**Goal**: Playwright로 핵심 사용자 플로우 E2E 테스트
**Status**: Pending

#### Tasks

**RED: Write Failing Tests First**
- [ ] **Test 6.1**: 퍼즐 생성 E2E 테스트
  - File: `tests/puzzle-generation.spec.ts`
  - Test Cases:
    - 페이지 로드 확인
    - "새 퍼즐 생성" 버튼 클릭
    - 그리드 렌더링 확인
    - 힌트 패널 표시 확인

- [ ] **Test 6.2**: 퍼즐 풀이 E2E 테스트
  - File: `tests/puzzle-solving.spec.ts`
  - Test Cases:
    - 셀 클릭 및 입력
    - 초성 힌트 토글
    - 정답 입력 시 완료 메시지 표시

**GREEN: Implement to Make Tests Pass**
- [ ] **Task 6.3**: E2E 테스트 환경 설정
  - Files: `playwright.config.ts`
  - Goal: Backend + Frontend 실행 상태에서 테스트
  - Details: 테스트 전 서버 실행 확인

- [ ] **Task 6.4**: 테스트 통과를 위한 수정
  - Files: 필요시 컴포넌트 수정
  - Goal: E2E 테스트 통과
  - Details: 접근성 속성 추가 등

**REFACTOR: Clean Up Code**
- [ ] **Task 6.5**: E2E 테스트 코드 정리
  - Files: tests/*.spec.ts
  - Checklist:
    - [ ] Page Object 패턴 적용
    - [ ] 재사용 가능한 헬퍼 함수

#### Quality Gate

**TDD Compliance**:
- [ ] 핵심 사용자 플로우 2개 이상 테스트
- [ ] 모든 E2E 테스트 통과

**Validation Commands**:
```bash
# Backend 실행 (별도 터미널)
./gradlew bootRun

# Frontend 실행 (별도 터미널)
cd frontend && npm run dev

# E2E 테스트
npx playwright test
npx playwright show-report
```

**Manual Test Checklist**:
- [ ] E2E 테스트 영상/스크린샷 확인
- [ ] 테스트 실행 시간 5분 이내

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| 기존 코드 회귀 발생 | Medium | High | 각 phase 후 수동 테스트, 점진적 리팩토링 |
| 한글 IME 테스트 복잡도 | High | Medium | E2E 테스트로 실제 브라우저 환경 검증 |
| DB 의존 테스트 불안정 | Medium | Medium | H2 인메모리 DB 사용, 테스트 데이터 격리 |
| Vitest 설정 오류 | Low | Low | 공식 문서 참조, 점진적 설정 |

---

## Rollback Strategy

### If Phase 2 Fails (Backend 리팩토링)
**Steps to revert**:
- 신규 클래스 삭제: GridUtils, PlacementValidator, GridConverter
- PuzzleGeneratorService 원래 코드 복구 (git checkout)
- 테스트 파일만 유지하여 추후 재시도

### If Phase 4-5 Fails (Frontend 리팩토링)
**Steps to revert**:
- 추출된 훅/유틸리티 삭제
- PuzzleGrid.tsx, HintPanel.tsx 원래 코드 복구
- 테스트 파일만 유지

---

## Progress Tracking

### Completion Status
- **Phase 1**: Complete (100%)
- **Phase 2**: Pending (0%)
- **Phase 3**: Pending (0%)
- **Phase 4**: Pending (0%)
- **Phase 5**: Pending (0%)
- **Phase 6**: Pending (0%)

**Overall Progress**: 17% complete (1/6 phases)

---

## Notes & Learnings

### Implementation Notes
- Phase 1: E2E 테스트가 이미 68개 존재함 (puzzle.spec.ts)
- Phase 1: React 19 + ESLint 새 규칙으로 인해 기존 코드에서 린트 오류 발생 (refs 접근, useEffect 내 setState)
- Phase 1: vitest/config에서 defineConfig를 import해야 vite.config.ts에서 test 설정 가능

### Blockers Encountered
- Phase 1: PuzzleGrid.tsx에서 렌더링 중 ref 접근 -> userInputs 상태만 사용하도록 수정
- Phase 1: useEffect 내 setState 린트 경고 -> eslint-disable 주석 추가 (Phase 5에서 리팩토링 예정)

### Improvements for Future Plans
- [To be filled after completion]

---

## References

### Documentation
- [Vitest Docs](https://vitest.dev/)
- [Testing Library Docs](https://testing-library.com/)
- [Playwright Docs](https://playwright.dev/)
- [JUnit 5 Docs](https://junit.org/junit5/)

### Related Files
- PuzzleGeneratorService: `src/main/java/.../service/PuzzleGeneratorService.java`
- PuzzleGrid: `frontend/src/components/PuzzleGrid.tsx`
- HintPanel: `frontend/src/components/HintPanel.tsx`

---

## Final Checklist

**Before marking plan as COMPLETE**:
- [ ] All phases completed with quality gates passed
- [ ] Full integration testing performed
- [ ] Documentation updated (CLAUDE.md 테스트 명령어 추가)
- [ ] Performance benchmarks meet targets
- [ ] All stakeholders notified
- [ ] Plan document archived for future reference

---

**Plan Status**: Pending
**Next Action**: Phase 1 - 테스트 환경 구축
**Blocked By**: None

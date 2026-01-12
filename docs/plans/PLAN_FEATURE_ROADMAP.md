# Implementation Plan: 십자말풀이 기능 확장 로드맵

**Status**: 📋 Planning
**Created**: 2026-01-08
**Last Updated**: 2026-01-08

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
표준국어대사전 API를 활용하여 단어 데이터를 대폭 확장하고, 퍼즐 알고리즘을 개선하며, 새로운 게임 기능을 추가하여 십자말풀이 앱의 완성도를 높입니다.

### Development Priority
| 순서 | 영역 | 목적 |
|------|------|------|
| 1️⃣ | **표준국어대사전 마이그레이션** | 데이터 소스 전환 (~5만 → ~30만 단어) |
| 2️⃣ | 퍼즐 알고리즘 개선 | 퍼즐 품질 향상 |
| 3️⃣ | TDD 리팩토링 완료 | 코드 품질 및 안정성 |
| 4️⃣ | 새 기능 개발 | 사용자 경험 향상 |

### Data Source Comparison

| 항목 | 한국어기초사전 (기존) | 표준국어대사전 (신규) |
|------|----------------------|----------------------|
| 총 단어 수 | ~5만 | **~50만** |
| 십자말풀이용 (2~8글자) | ~7,400 | **~30만 (추정)** |
| 다국어 번역 | ✅ 11개 언어 | ❌ 없음 |
| 예문 | ✅ 있음 | ✅ 있음 |
| 어원/한자 | ✅ 있음 | ✅ 있음 |
| 발음 | ✅ 있음 | ✅ 있음 |
| 전문 분야 | 제한적 | **67개 분야** |
| 단어 유형 | 제한적 | **고유어/한자어/외래어/혼종어** |
| 어휘 관계 | ❌ 없음 | **유의어/반의어/상위어/하위어** |
| 라이선스 | 공공누리 1유형 | 공공누리 1유형 |

### Success Criteria
- [ ] 표준국어대사전 데이터 10만 단어 이상 임포트
- [ ] 새 엔티티로 퍼즐 생성 정상 동작
- [ ] 기존 Word 엔티티 deprecated 처리
- [ ] 퍼즐 단어 밀도 50% 이상 달성
- [ ] 전체 테스트 커버리지 90% 이상

---

## Phase A: 표준국어대사전 마이그레이션
**Goal**: 표준국어대사전 API로 데이터 소스 전환, 새 엔티티 설계 및 임포트
**Status**: Pending
**Estimated Duration**: 1.5주

### A-1. 새 엔티티 설계

#### A-1.1 엔티티 구조

**새로운 엔티티 (std_ 접두사)**:
```
StdWord (NEW - 메인 단어 엔티티)
├── id: Long (PK)
├── targetCode: String (UNIQUE, 표준국어대사전 고유 코드)
├── word: String (표제어)
├── supNo: Integer (어깨번호, 동음이의어 구분)
├── length: Integer (글자 수)
├── firstChar: String (첫 글자, 인덱싱용)
├── wordType: String (고유어/한자어/외래어/혼종어)
├── origin: String (어원)
├── pronunciation: String (발음)
├── allomorph: String (이형태)
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
└── senses: List<StdSense>

StdSense (NEW - 의미/뜻풀이)
├── id: Long (PK)
├── word: StdWord (FK)
├── senseCode: String (의미 고유 코드)
├── senseOrder: Integer (의미 순서)
├── pos: String (품사)
├── category: String (전문 분야)
├── definition: String (뜻풀이)
├── type: String (일반어/전문어/방언 등)
├── examples: List<StdExample>
└── relations: List<StdWordRelation>

StdExample (NEW - 용례)
├── id: Long (PK)
├── sense: StdSense (FK)
├── example: String (용례 문장)
├── source: String (출전)
├── translation: String (번역, 한문 등)
└── origin: String (원문)

StdWordRelation (NEW - 어휘 관계)
├── id: Long (PK)
├── sense: StdSense (FK)
├── relationType: String (비슷한말/반대말/상위어/하위어/참고)
├── relatedWord: String (관련 단어)
├── relatedTargetCode: String (관련 단어 코드)
└── link: String (링크 URL)
```

#### A-1.2 인덱스 설계

```sql
-- 퍼즐 생성에 필수적인 인덱스
CREATE INDEX idx_std_word_length ON std_word(length);
CREATE INDEX idx_std_word_first_char ON std_word(first_char);
CREATE INDEX idx_std_word_first_char_length ON std_word(first_char, length);
CREATE INDEX idx_std_word_word_type ON std_word(word_type);
CREATE INDEX idx_std_word_target_code ON std_word(target_code);

-- 의미/분야 검색용
CREATE INDEX idx_std_sense_category ON std_sense(category);
CREATE INDEX idx_std_sense_pos ON std_sense(pos);

-- 어휘 관계 검색용
CREATE INDEX idx_std_relation_type ON std_word_relation(relation_type);
```

#### Tasks

- [ ] **Task A-1.1**: StdWord 엔티티 생성
  - File: `src/main/java/com/hakno/WordPuzzle/entity/StdWord.java`
  - Fields: targetCode, word, supNo, length, firstChar, wordType, origin, pronunciation, allomorph
  - Indexes: length, firstChar, wordType, targetCode

- [ ] **Task A-1.2**: StdSense 엔티티 생성
  - File: `src/main/java/com/hakno/WordPuzzle/entity/StdSense.java`
  - Fields: senseCode, senseOrder, pos, category, definition, type
  - Relation: @ManyToOne StdWord

- [ ] **Task A-1.3**: StdExample 엔티티 생성
  - File: `src/main/java/com/hakno/WordPuzzle/entity/StdExample.java`
  - Fields: example, source, translation, origin
  - Relation: @ManyToOne StdSense

- [ ] **Task A-1.4**: StdWordRelation 엔티티 생성
  - File: `src/main/java/com/hakno/WordPuzzle/entity/StdWordRelation.java`
  - Fields: relationType, relatedWord, relatedTargetCode, link
  - Relation: @ManyToOne StdSense

- [ ] **Task A-1.5**: Flyway 마이그레이션 스크립트
  - File: `src/main/resources/db/migration/V2__create_std_tables.sql`
  - DDL: CREATE TABLE statements for all std_ tables

### A-2. Repository 레이어

- [ ] **Task A-2.1**: StdWordRepository 생성
  - File: `src/main/java/com/hakno/WordPuzzle/repository/StdWordRepository.java`
  - Methods:
    ```java
    // 퍼즐 생성용 쿼리
    List<StdWord> findByLengthBetween(int min, int max);
    List<StdWord> findByFirstCharAndLengthBetween(String firstChar, int min, int max);
    List<StdWord> findByWordContaining(String character);
    
    // 랜덤 단어 (MySQL RAND())
    @Query("SELECT w FROM StdWord w WHERE w.length BETWEEN :min AND :max ORDER BY RAND()")
    List<StdWord> findRandomWords(int min, int max, Pageable pageable);
    
    // 분야별 필터
    @Query("SELECT DISTINCT w FROM StdWord w JOIN w.senses s WHERE s.category = :category")
    List<StdWord> findByCategory(String category);
    
    // 단어 유형별 필터
    List<StdWord> findByWordType(String wordType);
    
    // 통계
    long countByLengthBetween(int min, int max);
    ```

- [ ] **Task A-2.2**: StdSenseRepository 생성
  - File: `src/main/java/com/hakno/WordPuzzle/repository/StdSenseRepository.java`

- [ ] **Task A-2.3**: StdWordRelationRepository 생성
  - File: `src/main/java/com/hakno/WordPuzzle/repository/StdWordRelationRepository.java`
  - Methods: findByRelationType, findSynonyms, findAntonyms

### A-3. 표준국어대사전 API 임포트 서비스

#### A-3.1 API 클라이언트

- [ ] **Task A-3.1**: StdictApiClient 생성
  - File: `src/main/java/com/hakno/WordPuzzle/client/StdictApiClient.java`
  - Config: `stdict.api.key` in application.properties
  - Methods:
    ```java
    public class StdictApiClient {
        private static final String SEARCH_URL = "https://stdict.korean.go.kr/api/search.do";
        private static final String VIEW_URL = "https://stdict.korean.go.kr/api/view.do";
        
        // 검색 API (목록 조회)
        public StdictSearchResponse search(StdictSearchRequest request);
        
        // 상세 API (단어 상세 정보)
        public StdictViewResponse getWordDetail(String targetCode);
    }
    ```

- [ ] **Task A-3.2**: API DTO 클래스들
  - Files:
    - `StdictSearchRequest.java` (검색 요청)
    - `StdictSearchResponse.java` (검색 응답)
    - `StdictViewResponse.java` (상세 응답)
  - Location: `src/main/java/com/hakno/WordPuzzle/client/dto/`

#### A-3.2 임포트 서비스

- [ ] **Task A-3.3**: StdictImportService 생성
  - File: `src/main/java/com/hakno/WordPuzzle/service/StdictImportService.java`
  - Features:
    ```java
    @Service
    public class StdictImportService {
        
        // 음절 수별 임포트 (핵심 메서드)
        public ImportResult importByLength(int length);
        
        // 전체 임포트 (2~8글자)
        public ImportResult importAll();
        
        // 특정 초성으로 시작하는 단어 임포트
        public ImportResult importByInitial(String initial, int length);
        
        // 단어 상세 정보 임포트 (예문, 어휘관계 포함)
        public void importWordDetail(String targetCode);
        
        // 임포트 진행률 조회
        public ImportProgress getProgress();
        
        // Rate limiting (100ms 간격)
        private void rateLimitDelay();
    }
    ```

- [ ] **Task A-3.4**: ImportProgress 클래스
  - File: `src/main/java/com/hakno/WordPuzzle/dto/ImportProgress.java`
  - Fields: totalExpected, imported, failed, currentPhase, startTime, estimatedCompletion

- [ ] **Task A-3.5**: 임포트 컨트롤러
  - File: `src/main/java/com/hakno/WordPuzzle/controller/StdictImportController.java`
  - Endpoints:
    ```
    POST /api/stdict/import/start          - 전체 임포트 시작 (비동기)
    POST /api/stdict/import/length/{len}   - 특정 길이 임포트
    GET  /api/stdict/import/progress       - 진행률 조회
    POST /api/stdict/import/stop           - 임포트 중단
    GET  /api/stdict/stats                 - 임포트된 데이터 통계
    ```

#### A-3.3 임포트 전략

```
[임포트 순서 - 십자말풀이 우선순위]

1단계: 2~4글자 단어 (가장 많이 사용)
  - 예상: ~15만 단어
  - 시간: ~1시간

2단계: 5~6글자 단어
  - 예상: ~10만 단어
  - 시간: ~40분

3단계: 7~8글자 단어
  - 예상: ~5만 단어
  - 시간: ~20분

[Rate Limiting]
- API 호출 간격: 100ms
- 배치 크기: 100개씩
- 에러 시 재시도: 3회, exponential backoff
```

### A-4. 서비스 레이어 전환

- [ ] **Task A-4.1**: StdPuzzleGeneratorService 생성
  - File: `src/main/java/com/hakno/WordPuzzle/service/StdPuzzleGeneratorService.java`
  - 기존 PuzzleGeneratorService 복사 후 StdWordRepository 사용하도록 수정
  - 새 기능: 분야별 필터, 단어유형별 필터

- [ ] **Task A-4.2**: PuzzleGeneratorService 인터페이스화
  - File: `src/main/java/com/hakno/WordPuzzle/service/PuzzleGeneratorInterface.java`
  - 기존 구현체와 새 구현체 모두 같은 인터페이스 구현

- [ ] **Task A-4.3**: 설정으로 구현체 선택
  - File: `application.properties`
  - Config: `puzzle.data-source=stdict` (stdict | krdict)
  - 조건부 빈 등록으로 런타임 선택

### A-5. 기존 엔티티 Deprecated 처리

- [ ] **Task A-5.1**: Word 엔티티에 @Deprecated 추가
  - File: `src/main/java/com/hakno/WordPuzzle/entity/Word.java`
  - 주석: "StdWord로 대체됨. Phase A 완료 후 삭제 예정"

- [ ] **Task A-5.2**: Definition 엔티티에 @Deprecated 추가
  - File: `src/main/java/com/hakno/WordPuzzle/entity/Definition.java`

- [ ] **Task A-5.3**: 기존 Repository deprecated 처리
  - Files: WordRepository.java
  - 주석 추가, 새 Repository 안내

- [ ] **Task A-5.4**: 기존 DataImportService deprecated 처리
  - File: `src/main/java/com/hakno/WordPuzzle/service/DataImportService.java`

### A-6. 힌트 시스템 확장

- [ ] **Task A-6.1**: HintType Enum 생성
  - File: `src/main/java/com/hakno/WordPuzzle/dto/HintType.java`
  - Values: CHOSUNG, EXAMPLE, ORIGIN, PRONUNCIATION, SYNONYM, ANTONYM, CATEGORY

- [ ] **Task A-6.2**: 힌트 서비스
  - File: `src/main/java/com/hakno/WordPuzzle/service/HintService.java`
  - Methods:
    ```java
    String getChosungHint(String word);
    List<String> getExampleHints(Long senseId);
    String getOriginHint(Long wordId);
    List<String> getSynonymHints(Long senseId);
    List<String> getAntonymHints(Long senseId);
    String getCategoryHint(Long senseId);
    ```

- [ ] **Task A-6.3**: 힌트 API
  - Endpoint: `GET /api/puzzle/hint?wordId={id}&type={type}`

### A-7. 테마별 퍼즐

- [ ] **Task A-7.1**: 카테고리 목록 API
  - Endpoint: `GET /api/categories`
  - Response: 67개 전문 분야 + 단어 수

- [ ] **Task A-7.2**: 테마별 퍼즐 생성
  - Endpoint: `GET /api/puzzle/generate?category=의학`
  - Endpoint: `GET /api/puzzle/generate?wordType=고유어`

- [ ] **Task A-7.3**: 프론트엔드 테마 선택 UI
  - File: `frontend/src/components/ThemeSelector.tsx`

#### Quality Gate

**Validation Commands**:
```bash
# 마이그레이션 실행
./gradlew flywayMigrate

# 테스트 데이터 임포트 (2글자만)
curl -X POST "http://localhost:8080/api/stdict/import/length/2"

# 임포트 진행률 확인
curl "http://localhost:8080/api/stdict/import/progress"

# 통계 확인
curl "http://localhost:8080/api/stdict/stats"

# 새 엔티티로 퍼즐 생성 테스트
curl "http://localhost:8080/api/puzzle/generate?wordCount=10"

# 테마별 퍼즐 테스트
curl "http://localhost:8080/api/puzzle/generate?category=의학&wordCount=5"

# 테스트 실행
./gradlew test
cd frontend && npm run test:run
```

**Checklist**:
- [ ] std_word 테이블 생성됨
- [ ] std_sense, std_example, std_word_relation 테이블 생성됨
- [ ] 2글자 단어 최소 1,000개 임포트됨
- [ ] 새 엔티티로 퍼즐 생성 동작
- [ ] 기존 엔티티에 @Deprecated 추가됨
- [ ] 힌트 시스템 동작 (초성, 예문, 유의어)
- [ ] 카테고리별 필터 동작

---

## Phase B: 퍼즐 알고리즘 개선
**Goal**: 더 밀도 높고 품질 좋은 퍼즐 생성
**Status**: Pending
**Estimated Duration**: 1주

### B-1. 알고리즘 분석 및 설계

#### 현재 알고리즘 문제점
| 문제 | 원인 | 영향 |
|------|------|------|
| 낮은 단어 밀도 | 그리디 배치, 조기 종료 | 빈 공간 많음 |
| 불균형 레이아웃 | 중앙 시작 후 확장만 | 한쪽으로 치우침 |
| 교차점 부족 | 첫 매칭에서 중단 | 단어 간 연결 약함 |

#### 개선 방향
1. **백트래킹**: 배치 실패 시 이전 단어 제거 후 재시도
2. **스코어링 시스템**: 교차점 수, 밀도, 균형도 점수화
3. **다중 시드**: 여러 시작점에서 생성 후 최고 점수 선택
4. **대칭 모드**: 전통적인 십자말풀이 스타일

### B-2. 스코어링 시스템

- [ ] **Task B-2.1**: PuzzleScorer 클래스 생성
  - File: `src/main/java/com/hakno/WordPuzzle/service/PuzzleScorer.java`
  - Metrics:
    ```java
    public class PuzzleScore {
        int intersectionCount;    // 교차점 수
        double density;           // 단어 밀도 (글자 수 / 그리드 크기)
        double balance;           // 균형도 (상하좌우 분포)
        double connectivity;      // 연결도 (고립 단어 패널티)
        int totalScore;           // 종합 점수
    }
    ```

- [ ] **Task B-2.2**: 밀도 계산 로직
  - Formula: `filledCells / (gridSize * gridSize)`
  - Target: 0.5 이상

- [ ] **Task B-2.3**: 균형도 계산 로직
  - Method: 그리드를 4등분하여 각 영역의 글자 수 비교
  - Target: 표준편차 최소화

### B-3. 백트래킹 알고리즘

- [ ] **Task B-3.1**: BacktrackingPuzzleGenerator 클래스
  - File: `src/main/java/com/hakno/WordPuzzle/service/BacktrackingPuzzleGenerator.java`
  - Algorithm:
    ```
    function generateWithBacktracking(words, grid, depth):
        if depth >= targetWordCount:
            return score(grid)
        
        for placement in findPlacements(grid):
            placeWord(grid, placement)
            result = generateWithBacktracking(words, grid, depth + 1)
            
            if result.score > threshold:
                return result
            
            removeWord(grid, placement)  // 백트래킹
        
        return currentBestResult
    ```

- [ ] **Task B-3.2**: 상태 스냅샷 및 복원
  - Method: `GridSnapshot` 클래스로 그리드 상태 저장/복원
  - Optimization: 변경된 셀만 저장

- [ ] **Task B-3.3**: 가지치기 (Pruning)
  - Conditions:
    - 현재 점수 + 남은 최대 점수 < 최고 점수
    - 연속 N회 개선 없음
    - 시간 제한 초과

### B-4. 다중 시드 전략

- [ ] **Task B-4.1**: 병렬 퍼즐 생성
  - Method: 여러 시작 단어로 동시 생성
  - Implementation: `CompletableFuture` 활용

- [ ] **Task B-4.2**: 최적 결과 선택
  - Logic: 모든 결과 중 최고 점수 퍼즐 반환
  - Fallback: 시간 초과 시 현재 최고 결과 반환

### B-5. 대칭 레이아웃 (선택적)

- [ ] **Task B-5.1**: SymmetricPuzzleGenerator
  - File: `src/main/java/com/hakno/WordPuzzle/service/SymmetricPuzzleGenerator.java`
  - Types: 180도 회전 대칭, 좌우 대칭, 상하 대칭
  - Constraint: 단어 배치 시 대칭 위치도 함께 고려

- [ ] **Task B-5.2**: 대칭 옵션 API
  - Endpoint: `GET /api/puzzle/generate?symmetric=true`

### B-6. 성능 최적화

- [ ] **Task B-6.1**: 단어 인덱싱 개선
  - Structure: 글자별 단어 맵 (캐싱)
  - Example: `{'가': [가나, 가방, ...], '나': [나라, 나무, ...]}`

- [ ] **Task B-6.2**: 배치 가능 위치 캐싱
  - Method: 변경된 영역만 재계산

- [ ] **Task B-6.3**: 타임아웃 설정
  - Default: 5초
  - Behavior: 시간 초과 시 현재 최선 결과 반환

#### Quality Gate

**Validation Commands**:
```bash
# 퍼즐 품질 테스트
./gradlew test --tests "*PuzzleScorerTest"
./gradlew test --tests "*BacktrackingTest"

# 성능 벤치마크
curl -w "@curl-format.txt" "http://localhost:8080/api/puzzle/generate?wordCount=15"

# 밀도 확인
curl "http://localhost:8080/api/puzzle/generate?wordCount=15" | jq '.density'
```

**Checklist**:
- [ ] 단어 밀도 50% 이상 달성
- [ ] 퍼즐 생성 시간 5초 이내
- [ ] 백트래킹으로 품질 향상 확인
- [ ] 스코어 API 응답에 포함

---

## Phase C: TDD 리팩토링 완료
**Goal**: 기존 TDD 계획의 Phase 5, 6 완료
**Status**: Pending (기존 67% → 100%)
**Estimated Duration**: 3-4일

### C-1. Frontend 커스텀 훅 테스트 (기존 Phase 5)

#### Tasks

- [ ] **Test C-1.1**: usePuzzleNavigation 훅 테스트
  - File: `frontend/src/__tests__/hooks/usePuzzleNavigation.test.ts`
  - Test Cases:
    - 셀 클릭 시 selectedCell 업데이트
    - 화살표 키 이동 (상/하/좌/우)
    - Space/Tab으로 방향 전환
    - 단어 경계에서 이동 제한

- [ ] **Test C-1.2**: usePuzzleInput 훅 테스트
  - File: `frontend/src/__tests__/hooks/usePuzzleInput.test.ts`
  - Test Cases:
    - 한글 입력 처리
    - IME Composition 이벤트
    - Backspace 삭제
    - 입력 후 자동 이동

- [ ] **Test C-1.3**: HintPanel 컴포넌트 테스트
  - File: `frontend/src/__tests__/components/HintPanel.test.tsx`
  - Test Cases:
    - 가로/세로 힌트 렌더링
    - 힌트 클릭 시 onWordClick 호출
    - 초성 토글 동작
    - 선택된 단어 하이라이트

- [ ] **Test C-1.4**: PuzzleGrid 컴포넌트 테스트
  - File: `frontend/src/__tests__/components/PuzzleGrid.test.tsx`
  - Test Cases:
    - 그리드 렌더링
    - 셀 클릭 이벤트
    - 정답/오답 스타일링
    - 완료 시 onComplete 호출

- [ ] **Task C-1.5**: usePuzzleNavigation 훅 추출
  - File: `frontend/src/hooks/usePuzzleNavigation.ts`
  - Extract from: `PuzzleGrid.tsx`
  - Functions: 셀 선택, 키보드 네비게이션

- [ ] **Task C-1.6**: usePuzzleInput 훅 추출
  - File: `frontend/src/hooks/usePuzzleInput.ts`
  - Extract from: `PuzzleGrid.tsx`
  - Functions: 입력 처리, IME 핸들링

- [ ] **Task C-1.7**: PuzzleGrid 리팩토링
  - File: `frontend/src/components/PuzzleGrid.tsx`
  - Goal: 추출된 훅 사용, 컴포넌트 단순화

### C-2. E2E 테스트 보강 (기존 Phase 6)

- [ ] **Test C-2.1**: 퍼즐 생성 플로우
  - File: `tests/puzzle-generation.spec.ts`
  - Scenarios:
    - 기본 퍼즐 생성
    - 테마별 생성 (Phase A 완료 후)
    - 단어 유형별 생성

- [ ] **Test C-2.2**: 퍼즐 풀이 플로우
  - File: `tests/puzzle-solving.spec.ts`
  - Scenarios:
    - 셀 입력 및 이동
    - 힌트 사용
    - 퍼즐 완료 감지

- [ ] **Test C-2.3**: 힌트 시스템 테스트
  - File: `tests/hint-system.spec.ts`
  - Scenarios:
    - 힌트 타입 전환 (초성, 예문, 유의어)
    - 어원/한자 힌트 표시

- [ ] **Task C-2.4**: Page Object 패턴 적용
  - File: `tests/pages/PuzzlePage.ts`
  - Methods: generatePuzzle, fillCell, selectHint, etc.

#### Quality Gate

**Validation Commands**:
```bash
# Frontend 테스트
cd frontend && npm run test:coverage

# E2E 테스트
npx playwright test

# 전체 커버리지 확인
./gradlew test jacocoTestReport
```

**Checklist**:
- [ ] Frontend 커버리지 80% 이상
- [ ] 훅 테스트 모두 통과
- [ ] 컴포넌트 테스트 모두 통과
- [ ] E2E 테스트 모두 통과

---

## Phase D: 새 기능 개발
**Goal**: 게임성 향상을 위한 새로운 기능 추가
**Status**: Pending
**Estimated Duration**: 1-2주

### D-1. 타이머 및 점수 시스템

(기존과 동일)

### D-2. 일일 퍼즐

(기존과 동일)

### D-3. 저장 및 불러오기

(기존과 동일)

### D-4. 리더보드 (선택적)

(기존과 동일)

### D-5. 모바일 최적화

(기존과 동일)

---

## Phase E: 레거시 정리 (Phase A 완료 후)
**Goal**: 기존 한국어기초사전 관련 코드 제거
**Status**: Pending
**Estimated Duration**: 2-3일

### E-1. 레거시 코드 제거

- [ ] **Task E-1.1**: Word 엔티티 삭제
  - File: `src/main/java/com/hakno/WordPuzzle/entity/Word.java`
  - Prerequisite: 모든 서비스가 StdWord 사용 확인

- [ ] **Task E-1.2**: Definition 엔티티 삭제
  - File: `src/main/java/com/hakno/WordPuzzle/entity/Definition.java`

- [ ] **Task E-1.3**: WordRepository 삭제
  - File: `src/main/java/com/hakno/WordPuzzle/repository/WordRepository.java`

- [ ] **Task E-1.4**: DataImportService 삭제
  - File: `src/main/java/com/hakno/WordPuzzle/service/DataImportService.java`

- [ ] **Task E-1.5**: 기존 PuzzleGeneratorService 삭제
  - 조건: StdPuzzleGeneratorService로 완전 대체 후

### E-2. 데이터베이스 정리

- [ ] **Task E-2.1**: 기존 테이블 삭제 마이그레이션
  - File: `src/main/resources/db/migration/V3__drop_legacy_tables.sql`
  - DDL:
    ```sql
    DROP TABLE IF EXISTS definition;
    DROP TABLE IF EXISTS word;
    ```

- [ ] **Task E-2.2**: /data 폴더 JSON 파일 삭제
  - 기존 한국어기초사전 JSON 파일들

### E-3. 문서 업데이트

- [ ] **Task E-3.1**: README.md 업데이트
  - 데이터 소스를 표준국어대사전으로 변경

- [ ] **Task E-3.2**: CLAUDE.md 업데이트
  - 새 엔티티 구조, API 정보 반영

- [ ] **Task E-3.3**: 저작권 표시 업데이트
  - "국립국어원 표준국어대사전" 출처 표기

#### Quality Gate

**Checklist**:
- [ ] 기존 엔티티 참조하는 코드 없음
- [ ] 모든 테스트 통과
- [ ] 퍼즐 생성 정상 동작
- [ ] 문서 업데이트 완료

---

## Architecture Decisions

| Decision | Rationale | Trade-offs |
|----------|-----------|------------|
| **새 엔티티 생성 (std_ 접두사)** | 기존 시스템 영향 없이 병행 개발 가능 | 일시적 코드 중복 |
| **API 기반 임포트** | 전체 데이터 덤프 대신 필요한 것만 | 임포트 시간 소요 |
| **단계적 마이그레이션** | 안전한 전환, 롤백 가능 | 개발 기간 증가 |
| 백트래킹 알고리즘 | 품질 향상 | 생성 시간 증가 |
| 스코어링 기반 선택 | 객관적 품질 평가 | 복잡도 증가 |

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **API Rate Limit** | Medium | High | 100ms 간격, 재시도 로직 |
| **API 키 만료/차단** | Low | High | 키 갱신 프로세스 문서화 |
| 데이터 임포트 시간 | High | Medium | 배치 처리, 진행률 표시 |
| 백트래킹 성능 저하 | Medium | High | 타임아웃, 가지치기 |
| 스키마 변경 롤백 | Low | High | Flyway 버전 관리 |

---

## Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase A | 1.5주 | API 키 발급 완료 |
| Phase B | 1주 | Phase A 완료 |
| Phase C | 3-4일 | Phase A, B 완료 |
| Phase D | 1-2주 | Phase A, B, C 완료 |
| Phase E | 2-3일 | Phase A~D 완료 및 안정화 |

**Total Estimated**: 4-5주

---

## Progress Tracking

### Completion Status
- **Phase A (표준국어대사전 마이그레이션)**: Pending (0%)
- **Phase B (알고리즘 개선)**: Pending (0%)
- **Phase C (TDD 완료)**: Pending (0%)
- **Phase D (새 기능)**: Pending (0%)
- **Phase E (레거시 정리)**: Pending (0%)

**Overall Progress**: 0% complete

---

## Notes & Learnings

### Implementation Notes
- (작업 진행하면서 기록)

### Blockers Encountered
- (문제 발생 시 기록)

---

## References

### 표준국어대사전 API
- [오픈 API 소개](https://stdict.korean.go.kr/openapi/openApiInfo.do)
- API Endpoints:
  - 검색: `https://stdict.korean.go.kr/api/search.do`
  - 상세: `https://stdict.korean.go.kr/api/view.do`
- 라이선스: 공공누리 제1유형

### 한국어기초사전 API (레거시)
- [공식 문서](https://krdict.korean.go.kr/openApi/openApiInfo)
- 라이선스: 공공누리 제1유형

### 십자말풀이 알고리즘
- [Crossword Generation Algorithm](https://www.cs.utexas.edu/~ml/papers/crossword-ecai-94.pdf)
- [Constraint Satisfaction Problems](https://en.wikipedia.org/wiki/Constraint_satisfaction_problem)

---

## Final Checklist

**Before marking plan as COMPLETE**:
- [ ] All phases completed with quality gates passed
- [ ] 표준국어대사전 데이터 10만+ 단어 임포트
- [ ] 기존 한국어기초사전 코드 완전 제거
- [ ] Full integration testing performed
- [ ] Documentation updated (README, CLAUDE.md)
- [ ] 저작권 표시 "국립국어원 표준국어대사전" 명시
- [ ] Performance benchmarks meet targets
- [ ] Mobile testing completed
- [ ] Plan document archived

---

**Plan Status**: Planning
**Next Action**: API 키 발급 → Phase A-1.1 StdWord 엔티티 생성
**Blocked By**: 표준국어대사전 API 키 발급

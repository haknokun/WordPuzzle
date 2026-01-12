# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Korean crossword puzzle (십자말풀이) web application. Backend generates puzzles by placing words that intersect at shared characters; frontend provides interactive grid UI for solving.

## Build & Run Commands

### Backend (Spring Boot)
```bash
./gradlew bootRun                    # Run server on port 8080
./gradlew build                      # Build project
./gradlew test                       # Run all tests
./gradlew test --tests "ClassName"   # Run single test class
./gradlew test --tests "*.methodName" # Run single test method
```

### Frontend (React + Vite)
```bash
cd frontend
npm install                # Install dependencies
npm run dev                # Dev server on port 5173
npm run build              # Production build
npm run lint               # ESLint check
npm run test               # Run vitest in watch mode
npm run test:run           # Run tests once
npm run test:coverage      # Run tests with coverage report
npx vitest run path/to/file.test.ts  # Run single test file
```

### E2E Tests (Playwright)
```bash
npx playwright test                    # Run all E2E tests (auto-starts frontend)
npx playwright test puzzle.spec.ts     # Run single test file
npx playwright test --project=chromium # Run in specific browser
npx playwright test --ui               # Open interactive UI mode
```

### Prerequisites
- Java 21
- MySQL database `wordpuzzle` on localhost:3306 (user: root, password: 1234)
- Node.js for frontend

## Architecture

### Backend Flow
```
PuzzleController (/api/puzzle/generate)
    → PuzzleGeneratorService.generatePuzzle(gridSize, wordCount, level, source, category, wordType)
        → source=default: WordRepository (한국어기초사전)
        → source=std: StdWordRepository (표준국어대사전)
        → Place first word at grid center (ACROSS)
        → Find intersecting placements for remaining words
        → Assign numbers separately for ACROSS/DOWN words
        → Convert char[][] grid to List<List<PuzzleCell>>
    → Return PuzzleResponse
```

### Puzzle Generation Algorithm (PuzzleGeneratorService)
1. First word placed horizontally at grid center
2. Subsequent words must intersect existing letters
3. `findIntersectionCandidates()` - 그리드에서 교차 가능한 위치 수집
4. `findWordsForIntersection()` - 해당 글자 포함 단어 검색 (SEARCH_LIMIT=100)
5. `calculateAllPlacements()` - 단어 내 모든 글자 위치에서 배치 시도
6. `canPlaceWord()` validates:
   - 범위 체크 (그리드 경계)
   - 단어 앞뒤 빈칸 확보
   - 교차점 글자 일치
   - 인접 규칙: 위아래(또는 좌우) 모두 글자 있을 때만 거부 (완화된 규칙)
7. Words numbered independently: ACROSS (1,2,3...) and DOWN (1,2,3...)

### Frontend Components
- `App.tsx`: State management (puzzle, selectedWord, completed, level, source), 양방향 동기화
- `PuzzleGrid.tsx`: Grid rendering, completion detection, `onWordSelect` 콜백 (훅 사용으로 159줄로 감소)
- `HintPanel.tsx`: Displays ACROSS/DOWN clues with chosung(초성) hint toggle, 자동 스크롤
- `ThemeSelector.tsx`: 카테고리(의미 범주) 및 어종(고유어/한자어/외래어) 필터 UI

### Frontend Custom Hooks
- `usePuzzleNavigation.ts`: 셀 선택, 키보드 네비게이션, 방향 전환
- `usePuzzleInput.ts`: 사용자 입력 처리, Korean IME 조합, 키보드 이벤트

### Data Model
- `Word` entity: word, length, firstChar, partOfSpeech, vocabularyLevel, List<Definition>
- `PuzzleCell`: row, col, letter, isBlank, acrossNumber, downNumber
- `PuzzleWord`: number, word, definition, startRow, startCol, direction (ACROSS/DOWN)

### API
- `GET /api/puzzle/generate?gridSize=15&wordCount=10&level=초급&source=std&category=...&wordType=...`
  - Generate puzzle
  - `source`: `default` (한국어기초사전) | `std` (표준국어대사전)
  - `level`: 초급/중급/고급/null (한국어기초사전만 지원)
  - `category`: 의미 범주 필터 (표준국어대사전)
  - `wordType`: 고유어/한자어/외래어/혼종어 (표준국어대사전)
- `GET /api/categories` - 표준국어대사전 카테고리 목록
- `GET /api/hints?wordId=...&type=...` - 힌트 조회 (DEFINITION/CHOSUNG/EXAMPLE/SYNONYM)
- `POST /api/import?path=...` - Import word data from JSON files

## Testing Architecture

### Backend Tests
- `src/test/.../unit/` - Unit tests (GridUtils, GridConverter, PlacementValidator, HintService)
- `src/test/.../service/` - Service tests (BacktrackingPuzzleGenerator, PuzzleScorer, WordCache)
- `src/test/.../util/` - Utility tests (GridSnapshot)
- `src/test/.../repository/` - Repository tests with H2 in-memory database
- `src/test/.../integration/` - Controller integration tests with @WebMvcTest

### Frontend Tests
- `frontend/src/__tests__/utils/` - Pure function tests (chosung, puzzleUtils)
- `frontend/src/__tests__/hooks/` - Custom hook tests (usePuzzleNavigation, usePuzzleInput)
- `frontend/src/__tests__/components/` - React component tests (PuzzleGrid, HintPanel)

### E2E Tests
- `tests/*.spec.ts` - Playwright tests
  - `puzzle.spec.ts` - 기본 퍼즐 기능
  - `difficulty.spec.ts` - 난이도 및 단어 수 설정
  - `completion.spec.ts` - 퍼즐 완성 및 UI 상태
  - `korean-ime.spec.ts` - 한글 복합 모음 입력
  - `grid-interaction.spec.ts` - 그리드 상호작용 (18개 테스트)
  - `hint-system.spec.ts` - 힌트 시스템 (12개 테스트)
- `tests/pages/PuzzlePage.ts` - Page Object 패턴
- Runs against 4 browsers: Chromium, Firefox, WebKit, Edge
- Auto-starts frontend dev server via webServer config
- Playwright config: retries=2, workers=4, timeout=60s (flaky 방지)

### Test Coverage
| 영역 | 테스트 수 | 커버리지 |
|------|----------|----------|
| Backend | 250 | 95% |
| Frontend | 142 | 96% |
| E2E | 73 (×4 browsers = 292) | - |
| **Total** | **465+** | - |

## Key Implementation Details

- Grid uses `char[][] → List<List<PuzzleCell>>` conversion for JSON serialization
- Frontend maintains separate `userInputs[][]` state from puzzle answer grid
- Cell numbers stored as `acrossNumber`/`downNumber` (both can exist on same cell)
- Word data in `/data/*.json` files, imported via DataImportService
- Deep copy for userInputs state: `prev.map(r => [...r])`
- Grid ↔ Hint 양방향 동기화: `onWordSelect` 콜백 + `scrollIntoView`

### Korean IME Handling
- `onCompositionStart`: `isComposing = true` 설정
- `onCompositionEnd`: 최종 글자 처리 + 다음 셀 이동
- `handleInput`: `isComposing` 중에는 무시 (복합 모음 조합 보호)
- 복합 모음 지원: 의(ㅇ+ㅡ+ㅣ), 왜(ㅇ+ㅗ+ㅐ), 귀(ㄱ+ㅜ+ㅣ) 등

### 난이도별 단어 분포
| 난이도 | 단어 수 |
|--------|---------|
| NULL (전체) | 7,417 |
| 고급 | 1,453 |
| 중급 | 1,423 |
| 초급 | 870 |

## 한국어기초사전 데이터 구조

### 파일 구성
- 총 11개 JSON 파일 (~920MB, ~51,957 단어)
- 파일명 형식: `{번호}_{단어수}_{날짜}.json`

### JSON 구조
```
LexicalResource.Lexicon.LexicalEntry[] (단어 배열)
├── Lemma.feat
│   └── writtenForm: "단어"
├── feat[] (단어 속성)
│   ├── partOfSpeech: 명사/동사/형용사/부사/조사/접사/품사 없음
│   ├── vocabularyLevel: 초급/중급/고급/없음
│   ├── homonymNumber: 동음이의어 번호
│   ├── origin: 어원 (한자 등)
│   ├── pronunciation: 발음
│   └── semanticCategory: 의미 범주
└── Sense[] (의미, 복수 가능)
    ├── feat.definition: "한국어 정의"
    ├── Equivalent[] (다국어 번역 - 11개 언어)
    │   ├── language: 영어/일본어/중국어/프랑스어/스페인어/러시아어
    │   │            아랍어/베트남어/타이어/인도네시아어/몽골어
    │   ├── lemma: 번역어
    │   └── definition: 번역 정의
    └── SenseExample[] (예문)
        ├── type: 구/문장/대화
        └── example: "예문 내용"
```

### 활용 가능 필드
| 필드 | 용도 |
|------|------|
| vocabularyLevel | 난이도별 퍼즐 (초급/중급/고급) |
| partOfSpeech | 품사별 필터 |
| Equivalent | 다국어 힌트 |
| SenseExample | 예문 힌트 |
| origin | 한자어 표시 |

## 표준국어대사전 API 연동

### 환경 설정
```properties
# application.properties
stdict.api.key=${STDICT_API_KEY:}  # 환경변수, 없으면 빈 값
stdict.api.search-url=https://stdict.korean.go.kr/api/search.do
stdict.api.view-url=https://stdict.korean.go.kr/api/view.do
```

### StdWord 엔티티
- `word`: 단어
- `definition`: 정의
- `pos`: 품사
- `category`: 의미 범주 (가정생활, 경제, 교육 등)
- `wordType`: 어종 (고유어, 한자어, 외래어, 혼종어)

### UI 동작
- 데이터 소스가 '표준국어대사전'일 때 난이도 선택 비활성화
- ThemeSelector로 카테고리/어종 필터링 가능

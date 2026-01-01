# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Korean crossword puzzle (십자말풀이) web application. Backend generates puzzles by placing words that intersect at shared characters; frontend provides interactive grid UI for solving.

## Build & Run Commands

### Backend (Spring Boot)
```bash
./gradlew bootRun          # Run server on port 8080
./gradlew build            # Build project
./gradlew test             # Run tests
```

### Frontend (React + Vite)
```bash
cd frontend
npm install                # Install dependencies
npm run dev                # Dev server on port 5173
npm run build              # Production build
npm run lint               # ESLint check
```

### Prerequisites
- Java 21
- MySQL database `wordpuzzle` on localhost:3306 (user: root, password: 1234)
- Node.js for frontend

## Architecture

### Backend Flow
```
PuzzleController (/api/puzzle/generate)
    → PuzzleGeneratorService.generatePuzzle(gridSize, wordCount, level)
        → WordRepository.findRandomWordsWithDefinitionsByLevel() (fetch candidates)
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
- `App.tsx`: State management (puzzle, selectedWord, completed, level)
- `PuzzleGrid.tsx`: Grid rendering, user input handling, keyboard navigation, completion detection
- `HintPanel.tsx`: Displays ACROSS/DOWN clues with chosung(초성) hint toggle

### Data Model
- `Word` entity: word, length, firstChar, partOfSpeech, vocabularyLevel, List<Definition>
- `PuzzleCell`: row, col, letter, isBlank, acrossNumber, downNumber
- `PuzzleWord`: number, word, definition, startRow, startCol, direction (ACROSS/DOWN)

### API
- `GET /api/puzzle/generate?gridSize=15&wordCount=10&level=초급` - Generate puzzle (level: 초급/중급/고급/null)
- `POST /api/import?path=...` - Import word data from JSON files

## Key Implementation Details

- Grid uses `char[][] → List<List<PuzzleCell>>` conversion for JSON serialization
- Frontend maintains separate `userInputs[][]` state from puzzle answer grid
- Cell numbers stored as `acrossNumber`/`downNumber` (both can exist on same cell)
- Word data in `/data/*.json` files, imported via DataImportService
- Korean IME handling with CompositionEvent (onCompositionStart/End)
- Deep copy for userInputs state: `prev.map(r => [...r])`

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

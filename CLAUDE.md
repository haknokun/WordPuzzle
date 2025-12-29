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
    → PuzzleGeneratorService.generatePuzzle(gridSize, wordCount)
        → WordRepository.findByLengthBetween() (fetch candidates from DB)
        → Place first word at grid center (ACROSS)
        → Find intersecting placements for remaining words
        → Assign numbers separately for ACROSS/DOWN words
        → Convert char[][] grid to List<List<PuzzleCell>>
    → Return PuzzleResponse
```

### Puzzle Generation Algorithm (PuzzleGeneratorService)
1. First word placed horizontally at grid center
2. Subsequent words must intersect existing letters
3. `canPlaceWord()` validates: bounds, no adjacent parallel words, required intersection
4. `findBestPlacement()` prioritizes placements with most intersections
5. Words numbered independently: ACROSS (1,2,3...) and DOWN (1,2,3...)

### Frontend Components
- `App.tsx`: State management (puzzle, selectedWord, completed)
- `PuzzleGrid.tsx`: Grid rendering, user input handling, keyboard navigation, completion detection
- `HintPanel.tsx`: Displays ACROSS/DOWN clues with selection highlight

### Data Model
- `Word` entity: word string + length + List<Definition>
- `PuzzleCell`: row, col, letter, isBlank, acrossNumber, downNumber
- `PuzzleWord`: number, word, definition, startRow, startCol, direction (ACROSS/DOWN)

### API
- `GET /api/puzzle/generate?gridSize=15&wordCount=10` - Generate new puzzle
- `POST /api/import?path=...` - Import word data from JSON files

## Key Implementation Details

- Grid uses `char[][] → List<List<PuzzleCell>>` conversion for JSON serialization
- Frontend maintains separate `userInputs[][]` state from puzzle answer grid
- Cell numbers stored as `acrossNumber`/`downNumber` (both can exist on same cell)
- Word data in `/data/*.json` files, imported via DataImportService

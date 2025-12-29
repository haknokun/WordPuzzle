# WordPuzzle (십자말풀이)

한국어 십자말풀이 웹 애플리케이션입니다. 단어들이 교차하는 퍼즐을 자동 생성하고, 인터랙티브한 UI로 풀 수 있습니다.

## 스크린샷

```
┌─────────────────────────────────────────────────┐
│                  십자말풀이                      │
│         [그리드 크기: 15] [단어 수: 10]          │
│                [새 퍼즐 생성]                    │
├────────────────────┬────────────────────────────┤
│   ┌─┬─┬─┬─┐       │  가로 열쇠                  │
│   │가│나│다│라│       │  1. 한글의 첫소리... [ㄱ] │
│   └─┴─┼─┴─┘       │  2. 물건을 사고...   [ㄱ] │
│       │마│           ├────────────────────────────┤
│       │바│           │  세로 열쇠                  │
│       └─┘           │  1. 위에서 아래로... [ㄱ] │
└────────────────────┴────────────────────────────┘
```

## 기능

- 자동 퍼즐 생성 (그리드 크기, 단어 수 설정 가능)
- 단어 교차 알고리즘 (단어들이 공유 글자에서 교차)
- 셀 클릭으로 가로/세로 전환
- 키보드 네비게이션 (화살표, 스페이스바, Backspace)
- 초성 힌트 기능
- 정답/오답 실시간 피드백
- 퍼즐 완료 감지

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3, Java 21, MySQL, Gradle |
| Frontend | React 18, TypeScript, Vite |

## 실행 방법

### 사전 요구사항

- Java 21
- Node.js 18+
- MySQL 8.0+

### 데이터베이스 설정

```sql
CREATE DATABASE wordpuzzle;
```

`src/main/resources/application.properties`에서 DB 설정 확인:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/wordpuzzle
spring.datasource.username=root
spring.datasource.password=1234
```

### Backend 실행

```bash
./gradlew bootRun
```
서버가 `http://localhost:8080`에서 실행됩니다.

### Frontend 실행

```bash
cd frontend
npm install
npm run dev
```
개발 서버가 `http://localhost:5173`에서 실행됩니다.

### 단어 데이터 임포트

`/data` 폴더에 JSON 파일을 준비한 후:
```bash
curl -X POST "http://localhost:8080/api/import?path=data/1_5000_20251204.json"
```

## API

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/puzzle/generate` | GET | 퍼즐 생성 |
| `/api/import` | POST | 단어 데이터 임포트 |

### 퍼즐 생성 예시

```bash
curl "http://localhost:8080/api/puzzle/generate?gridSize=15&wordCount=10"
```

## 프로젝트 구조

```
WordPuzzle/
├── src/main/java/com/hakno/WordPuzzle/
│   ├── controller/      # REST API 컨트롤러
│   ├── service/         # 비즈니스 로직 (퍼즐 생성)
│   ├── entity/          # JPA 엔티티
│   ├── dto/             # 데이터 전송 객체
│   └── repository/      # 데이터 접근 계층
├── frontend/
│   ├── src/
│   │   ├── components/  # React 컴포넌트
│   │   ├── api/         # API 호출
│   │   └── types/       # TypeScript 타입
│   └── ...
└── data/                # 단어 데이터 (JSON)
```

## 데이터 출처

본 프로젝트는 **국립국어원 한국어기초사전**의 데이터를 사용합니다.

- 출처: [한국어기초사전](https://krdict.korean.go.kr/)
- 제공: 국립국어원
- 라이선스: [공공누리 제1유형](https://www.kogl.or.kr/info/license.do) (출처표시)

> 이 저작물은 공공누리 제1유형에 따라 국립국어원의 공공저작물을 이용하였습니다.

## 라이선스

MIT License (소스코드)

단어 데이터는 공공누리 제1유형 라이선스를 따릅니다.

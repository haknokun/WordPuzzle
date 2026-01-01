#!/bin/bash
# Auto-test hook: 파일 수정 후 자동으로 관련 테스트 실행

# stdin에서 JSON 입력 읽기
input=$(cat)

# Python으로 JSON 파싱 (jq 대신)
file_path=$(echo "$input" | python -c "import sys, json; d=json.load(sys.stdin); print(d.get('tool_input', {}).get('file_path', ''))" 2>/dev/null)

# 파일 경로가 없으면 종료
if [ -z "$file_path" ]; then
  exit 0
fi

# Java 파일 수정 시 Gradle 테스트 실행
if [[ "$file_path" == *.java ]]; then
  echo "🧪 Running Gradle tests..."
  cd "$CLAUDE_PROJECT_DIR"

  # 테스트 파일인 경우 해당 테스트만 실행
  if [[ "$file_path" == *Test.java ]]; then
    # 테스트 클래스명 추출
    test_class=$(basename "$file_path" .java)
    ./gradlew test --tests "*$test_class" --quiet 2>&1 | tail -10
  else
    # 전체 테스트 실행 (빠른 실패 모드)
    ./gradlew test --quiet --fail-fast 2>&1 | tail -10
  fi

  if [ $? -eq 0 ]; then
    echo "✅ Tests passed"
  else
    echo "❌ Tests failed"
  fi
fi

# TypeScript/TSX 파일 수정 시 Vitest 실행
if [[ "$file_path" == *.ts ]] || [[ "$file_path" == *.tsx ]]; then
  # 테스트 파일이 아닌 경우만 실행
  if [[ "$file_path" != *.test.ts* ]] && [[ "$file_path" != *__tests__* ]]; then
    echo "🧪 Running Vitest..."
    cd "$CLAUDE_PROJECT_DIR/frontend"
    npm run test:run 2>&1 | tail -10

    if [ $? -eq 0 ]; then
      echo "✅ Tests passed"
    else
      echo "❌ Tests failed"
    fi
  fi
fi

exit 0

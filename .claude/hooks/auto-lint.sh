#!/bin/bash
# Auto-lint hook: Frontend 파일 수정 후 ESLint 자동 실행

# stdin에서 JSON 입력 읽기
input=$(cat)

# Python으로 JSON 파싱 (jq 대신)
file_path=$(echo "$input" | python -c "import sys, json; d=json.load(sys.stdin); print(d.get('tool_input', {}).get('file_path', ''))" 2>/dev/null)

# 파일 경로가 없으면 종료
if [ -z "$file_path" ]; then
  exit 0
fi

# Frontend TypeScript/TSX 파일인 경우 ESLint 실행
if [[ "$file_path" == *frontend*.ts ]] || [[ "$file_path" == *frontend*.tsx ]]; then
  echo "🔍 Running ESLint..."
  cd "$CLAUDE_PROJECT_DIR/frontend"

  # ESLint 실행 (자동 수정 포함)
  npx eslint "$file_path" --fix 2>&1

  if [ $? -eq 0 ]; then
    echo "✅ Lint passed"
  else
    echo "⚠️ Lint issues found (auto-fixed where possible)"
  fi
fi

exit 0

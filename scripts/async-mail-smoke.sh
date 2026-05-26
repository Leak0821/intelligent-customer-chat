#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://127.0.0.1:8080}"
message_id="async-mail-smoke-$(date +%s)"
thread_id="async-thread-$(date +%s)"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required but not found" >&2
  exit 1
fi

call() {
  local method="$1"
  local path="$2"
  local body="${3:-}"

  echo "===== ${method} ${path} ====="
  if [[ -n "$body" ]]; then
    curl -fsS -X "$method" "$base_url$path" -H 'Content-Type: application/json' -d "$body"
  else
    curl -fsS -X "$method" "$base_url$path"
  fi
  echo
  echo
}

payload="{\"messageId\":\"${message_id}\",\"threadId\":\"${thread_id}\",\"from\":\"buyer@example.com\",\"subject\":\"Need async logistics help\",\"body\":\"Please check order ABCD1234 and tracking number ZXCV9876 when the queue processor runs.\"}"

call POST "/api/mail/manual-enqueue" "$payload"
call GET "/api/mail/overview?recentLimit=5"
call POST "/api/mail/process-pending?limit=5"
call GET "/api/mail/receipts?limit=5"
call GET "/api/workflows/by-message/${message_id}/replay"

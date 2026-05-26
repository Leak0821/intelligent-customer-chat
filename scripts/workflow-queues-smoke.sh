#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://127.0.0.1:8080}"
message_id="queue-smoke-$(date +%s)"
thread_id="queue-thread-$(date +%s)"

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

payload="{\"messageId\":\"${message_id}\",\"threadId\":\"${thread_id}\",\"from\":\"customer@example.com\",\"subject\":\"Need logistics help\",\"body\":\"Please check order ABCD1234 and tracking number ZXCV9876.\"}"

demo_response="$(curl -fsS -X POST "$base_url/api/workflows/demo" -H 'Content-Type: application/json' -d "$payload")"
echo "===== POST /api/workflows/demo ====="
echo "$demo_response"
echo
echo

run_id="$(printf '%s' "$demo_response" | sed -n 's/.*"runId":"\([^"]*\)".*/\1/p' | head -n 1)"
if [[ -z "$run_id" ]]; then
  echo "Failed to parse runId from demo response" >&2
  exit 1
fi

call GET "/api/workflows/queues/review?limit=10"
call POST "/api/workflows/${run_id}/approve-send" '{"reviewer":"queue-smoke","approvalNote":"approved from queue smoke"}'
call GET "/api/workflows/queues/review?limit=10"
call GET "/api/workflows/queues/dispatch?limit=10"

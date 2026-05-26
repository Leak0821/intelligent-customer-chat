#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://127.0.0.1:8080}"
scenario="${1:-after-sales}"

require_curl() {
  if ! command -v curl >/dev/null 2>&1; then
    echo "curl is required but not found" >&2
    exit 1
  fi
}

call() {
  local method="$1"
  local path="$2"
  local body="${3:-}"

  if [[ -n "$body" ]]; then
    curl -fsS -X "$method" "$base_url$path" -H 'Content-Type: application/json' -d "$body"
  else
    curl -fsS -X "$method" "$base_url$path"
  fi
  echo
}

smoke_after_sales() {
  local payload='{"messageId":"smoke-after-sales-001","threadId":"smoke-thread-001","from":"customer@example.com","subject":"Need help with tracking","body":"Hi, my order ABCD1234 tracking number is ZXCV9876. Could you check the latest logistics status?"}'
  local demo_response run_id

  demo_response="$(curl -fsS -X POST "$base_url/api/workflows/demo" -H 'Content-Type: application/json' -d "$payload")"
  echo "$demo_response"
  run_id="$(printf '%s' "$demo_response" | sed -n 's/.*"runId":"\([^"]*\)".*/\1/p' | head -n 1)"

  if [[ -z "$run_id" ]]; then
    echo "Failed to parse runId from demo response" >&2
    exit 1
  fi

  call GET "/api/workflows/$run_id/replay"
  call GET "/api/workflows/$run_id/evaluation"
  call GET "/api/workflows/$run_id/events"
  call GET "/api/workflows/$run_id/draft"
}

smoke_pre_sales() {
  local payload='{"messageId":"smoke-pre-sales-001","threadId":"smoke-thread-002","from":"customer@example.com","subject":"Need recommendation","body":"Hi, I am looking for a product recommendation for my living room."}'
  local demo_response run_id

  demo_response="$(curl -fsS -X POST "$base_url/api/workflows/demo" -H 'Content-Type: application/json' -d "$payload")"
  echo "$demo_response"
  run_id="$(printf '%s' "$demo_response" | sed -n 's/.*"runId":"\([^"]*\)".*/\1/p' | head -n 1)"

  if [[ -z "$run_id" ]]; then
    echo "Failed to parse runId from demo response" >&2
    exit 1
  fi

  call GET "/api/workflows/$run_id/replay"
  call GET "/api/workflows/$run_id/evaluation"
  call GET "/api/workflows/$run_id/events"
  call GET "/api/workflows/$run_id/draft"
}

require_curl
call GET "/api/runtime-config/preflight"
call GET "/api/runtime-config"
call GET "/api/business/orders"
call GET "/api/business/logistics"
call GET "/api/business/policies"
call GET "/api/knowledge/seeds"

case "$scenario" in
  after-sales)
    smoke_after_sales
    ;;
  pre-sales)
    smoke_pre_sales
    ;;
  *)
    echo "Usage: $0 [after-sales|pre-sales]" >&2
    exit 1
    ;;
esac

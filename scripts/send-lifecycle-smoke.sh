#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://127.0.0.1:8080}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required but not found" >&2
  exit 1
fi

payload='{"messageId":"send-smoke-after-sales-001","threadId":"send-smoke-thread-001","from":"customer@example.com","subject":"Need help with tracking","body":"Hi, my order ABCD1234 tracking number is ZXCV9876. Could you check the latest logistics status?"}'

demo_response="$(curl -fsS -X POST "$base_url/api/workflows/demo" -H 'Content-Type: application/json' -d "$payload")"
echo "$demo_response"

run_id="$(printf '%s' "$demo_response" | sed -n 's/.*"runId":"\([^"]*\)".*/\1/p' | head -n 1)"
if [[ -z "$run_id" ]]; then
  echo "Failed to parse runId from demo response" >&2
  exit 1
fi

curl -fsS -X POST "$base_url/api/workflows/$run_id/approve-send" -H 'Content-Type: application/json' -d '{"reviewer":"smoke-bot","approvalNote":"approved by local send smoke"}'
echo
curl -fsS -X POST "$base_url/api/workflows/$run_id/dispatch"
echo
curl -fsS "$base_url/api/workflows/$run_id/replay"
echo
curl -fsS "$base_url/api/workflows/$run_id/dispatches"
echo
curl -fsS "$base_url/api/workflows/$run_id/reviews"

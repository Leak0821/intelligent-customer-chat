#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://127.0.0.1:8080}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required but not found" >&2
  exit 1
fi

call() {
  local path="$1"
  echo "===== GET ${path} ====="
  curl -fsS "$base_url$path"
  echo
  echo
}

call "/api/workflows/evaluations/summary?limit=20&scene=AFTER_SALES"
call "/api/workflows/evaluations/summary?limit=20&scene=AFTER_SALES&businessFactStatus=INSUFFICIENT_INPUT"
call "/api/workflows/evaluations/recent?limit=20&knowledgeRole=knowledge%20supplements%20explanation%20and%20expectation%20setting%20around%20the%20current%20business%20facts"
call "/api/workflows/evaluations/recent?limit=20&businessFactRole=business%20facts%20are%20blocked%20until%20key%20identifiers%20are%20provided"
call "/api/workflows/evaluations/recent?limit=20&replyFallbackReason=follow_up_template_required"

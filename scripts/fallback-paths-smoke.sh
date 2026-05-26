#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://127.0.0.1:8080}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required but not found" >&2
  exit 1
fi

call() {
  local method="$1"
  local path="$2"
  echo "===== ${method} ${path} ====="
  curl -fsS -X "$method" "$base_url$path"
  echo
  echo
}

call POST "/api/workflows/demo/scenarios/after-sales-missing-id?mode=analysis"
call POST "/api/workflows/demo/scenarios/after-sales-manual-review?mode=analysis"
call POST "/api/workflows/demo/scenarios/system-blocked-demo?mode=replay"

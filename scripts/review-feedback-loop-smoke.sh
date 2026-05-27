#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://127.0.0.1:8080}"
scenario_id="${SCENARIO_ID:-after-sales-manual-review}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required but not found" >&2
  exit 1
fi

curl -fsS -X POST "$base_url/api/workflows/demo/scenarios/$scenario_id?mode=review_loop"
echo

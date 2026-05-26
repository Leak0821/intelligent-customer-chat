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

call GET "/api/business/orders/ABCD1234"
call GET "/api/business/logistics/ZXCV9876"
call GET "/api/business/policies/by-intent/logistics_tracking"
call POST "/api/business/facts/preview" '{"customerEmail":"buyer@example.com","scene":"AFTER_SALES","subIntent":"logistics_tracking","orderId":"ABCD1234","trackingNumber":"ZXCV9876","queryReason":"manual smoke preview"}'

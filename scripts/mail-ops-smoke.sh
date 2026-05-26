#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://127.0.0.1:8080}"
process_limit="${PROCESS_LIMIT:-5}"
recent_limit="${RECENT_LIMIT:-10}"

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

call GET "/api/mail/overview?recentLimit=${recent_limit}"
call POST "/api/mail/poll"
call POST "/api/mail/process-pending?limit=${process_limit}"
call GET "/api/mail/overview?recentLimit=${recent_limit}"
call GET "/api/mail/receipts?limit=${recent_limit}"

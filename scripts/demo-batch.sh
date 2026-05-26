#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://127.0.0.1:8080}"
scenario_dir="${SCENARIO_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/ops/demo-scenarios}"
mode="${1:-analysis}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required but not found" >&2
  exit 1
fi

if [[ ! -d "$scenario_dir" ]]; then
  echo "Scenario directory not found: $scenario_dir" >&2
  exit 1
fi

run_request() {
  local endpoint="$1"
  local file="$2"
  echo "===== $(basename "$file") -> $endpoint ====="
  curl -fsS -X POST "$base_url$endpoint" \
    -H 'Content-Type: application/json' \
    --data-binary "@$file"
  echo
  echo
}

case "$mode" in
  analysis)
    endpoint="/api/workflows/demo/analysis"
    ;;
  replay)
    endpoint="/api/workflows/demo/replay"
    ;;
  run)
    endpoint="/api/workflows/demo"
    ;;
  *)
    echo "Usage: $0 [analysis|replay|run]" >&2
    exit 1
    ;;
esac

for file in "$scenario_dir"/*.json; do
  run_request "$endpoint" "$file"
done

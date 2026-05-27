#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://127.0.0.1:8080}"
scenario_dir="${SCENARIO_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/ops/demo-scenarios}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required but not found" >&2
  exit 1
fi

if [[ ! -d "$scenario_dir" ]]; then
  echo "Scenario directory not found: $scenario_dir" >&2
  exit 1
fi

run_validation() {
  local scenario_id="$1"
  echo "===== ${scenario_id} -> validate ====="
  curl -fsS -X POST "$base_url/api/workflows/demo/scenarios/${scenario_id}?mode=validate"
  echo
  echo
}

for file in "$scenario_dir"/*.json; do
  scenario_id="$(basename "$file" .json)"
  run_validation "$scenario_id"
done

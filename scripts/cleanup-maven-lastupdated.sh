#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
m2_repo="${M2_REPO:-$repo_root/.m2/repository}"

if [[ ! -d "$m2_repo" ]]; then
  echo "Maven repository not found: $m2_repo" >&2
  exit 1
fi

deleted_count="$(find "$m2_repo" -name '*.lastUpdated' -type f -print -delete | wc -l | tr -d ' ')"
echo "Removed ${deleted_count} stale Maven lastUpdated marker(s) from $m2_repo"

#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
config_dir="${NACOS_CONFIG_DIR:-$repo_root/ops/nacos/runtime-config}"
nacos_addr="${NACOS_ADDR:-127.0.0.1:8848}"
nacos_group="${NACOS_GROUP:-DEFAULT_GROUP}"
nacos_namespace="${NACOS_NAMESPACE:-}"

if [[ ! -d "$config_dir" ]]; then
  echo "Nacos config directory not found: $config_dir" >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required but not found" >&2
  exit 1
fi

publish_config() {
  local data_id="$1"
  local file_path="$2"
  local endpoint="http://${nacos_addr}/nacos/v1/cs/configs"
  local response
  local verify
  local curl_args=(
    -sS
    -X POST
    "$endpoint"
    --data-urlencode "dataId=${data_id}"
    --data-urlencode "group=${nacos_group}"
    --data-urlencode "type=json"
    --data-urlencode "content@${file_path}"
  )

  if [[ -n "$nacos_namespace" ]]; then
    curl_args+=(--data-urlencode "tenant=${nacos_namespace}")
  fi

  response="$(curl "${curl_args[@]}")"
  if [[ "$response" != "true" ]]; then
    echo "Failed to publish ${data_id}: ${response}" >&2
    exit 1
  fi

  local verify_url="${endpoint}?dataId=${data_id}&group=${nacos_group}"
  if [[ -n "$nacos_namespace" ]]; then
    verify_url="${verify_url}&tenant=${nacos_namespace}"
  fi
  verify="$(curl -sS "$verify_url")"
  if [[ -z "$verify" ]]; then
    echo "Published ${data_id}, but verification returned empty content" >&2
    exit 1
  fi

  echo "Published ${data_id}"
}

publish_config "agent-prompts.json" "$config_dir/agent-prompts.json"
publish_config "agent-intents.json" "$config_dir/agent-intents.json"
publish_config "agent-retrieval.json" "$config_dir/agent-retrieval.json"

echo "Nacos runtime configs are ready at ${nacos_addr} / group=${nacos_group}"

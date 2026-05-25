#!/usr/bin/env bash

# 仅在当前仓库内启用 Codex 本地环境，避免污染全局配置。
# 用法：source ./scripts/use-local-codex.sh

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  echo "请用 source 方式加载：source ./scripts/use-local-codex.sh" >&2
  exit 1
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export CODEX_HOME="$repo_root/.codex-home"
export PATH="$repo_root/.codex-local/bin:$PATH"

echo "已启用项目内 Codex 环境"
echo "CODEX_HOME=$CODEX_HOME"

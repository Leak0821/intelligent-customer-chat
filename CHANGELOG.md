# 变更记录

> 记录仓库中的主要修改，方便回看“什么时候改了什么”。

## 2026-05-25

### 分支

- `main`
- `feat/codex-ai-plugins`
- `init-agent-playbook`

### 修改点

- 初始化 Git 仓库并关联 GitHub
- 清理过早创建的占位目录，保留最小仓库骨架
- 将 README 改成中文 Demo 说明，明确不用于生产
- 在项目内配置了 `oh-my-codex`、`superpowers` 和 `OpenSpec`
- 添加了仅作用于当前仓库的本地启动脚本 `scripts/use-local-codex.sh`
- 补充分支管理记录，说明特性分支保留策略
- 初始化 `AGENTS.md`，约束 Agent 默认工作方式
- 新增项目级最佳实践文档与仓库入库边界说明
- 新增 `research/` 与 `openspec/` 的目录说明文件
- 更新 README，补充规范文档入口
- 更新 `.gitignore`，忽略本地环境变量文件和本地参考 PDF
- 补充 AI 工作流文档，明确 `oh-my-codex`、`superpowers`、OpenSpec、`gstack` 的职责和阶段门禁
- 补充与技术栈无关的工程实现原则

### 结果

- 已完成本地 AI 插件环境的第一版搭建
- 已记录当前仓库的分支管理和变更记录方式
- 已形成智能客服项目的文档先行骨架
- 已明确当前仓库中哪些 AI 工具相关目录只保留本地

### 记录模板

以后新增记录时，按下面格式补充即可：

```md
## YYYY-MM-DD

### 分支

- `feat/xxx`

### 修改点

- 修改了什么
- 影响了什么

### 结果

- 是否已推送到远端
- 是否已合并
```

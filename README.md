# 智能客服项目

这是一个用于练手的智能客服 Demo 仓库。

## 项目说明

- 当前仅用于学习、实验和版本管理
- 暂不引入固定技术栈或框架
- 目标是先把仓库结构、协作流程和迭代记录跑通

## 重要提醒

- 本项目不用于生产环境
- 任何实现、接口和数据结构都可能随练习过程调整
- 后续会根据实际需要逐步补充功能

## 当前状态

- 已完成 Git 仓库初始化并关联 GitHub
- 已建立基础的版本管理流程
- 已初始化一版 AI 协作规范和文档骨架
- 目前没有正式业务代码

## 文档入口

- [AGENTS.md](./AGENTS.md)：仓库级 Agent 执行合同
- [docs/agent-playbook.md](./docs/agent-playbook.md)：项目级最佳实践初稿
- [docs/ai-workflow.md](./docs/ai-workflow.md)：`oh-my-codex + superpowers + OpenSpec + gstack` 工作流标准
- [docs/engineering-standards.md](./docs/engineering-standards.md)：与技术栈无关的实现原则
- [docs/repository-boundaries.md](./docs/repository-boundaries.md)：哪些内容可以入库，哪些只留本地
- [research/README.md](./research/README.md)：探索材料放置位置
- [openspec/changes/README.md](./openspec/changes/README.md)：进行中的正式变更合同
- [openspec/specs/README.md](./openspec/specs/README.md)：稳定规格沉淀位置

## 后续计划

- 继续确认智能客服的业务范围和第一条闭环链路
- 再逐步确定技术栈和系统边界
- 再逐步实现智能客服相关能力

## 本地环境

如果需要在当前项目内启用 Codex 本地环境，请在仓库根目录执行：

```bash
source ./scripts/use-local-codex.sh
```

这只会影响当前 shell，会把 `CODEX_HOME` 指向本仓库内的 `.codex-home`。

## 分支管理记录

- 这类练习项目建议保留特性分支，方便回看每次修改
- 分支命名建议使用 `feat/`、`fix/`、`chore/` 前缀
- 当前仓库会按正常 Git flow 记录每次分支开发和合并过程
- 如果某个分支已经合并但还想保留历史，可以先不删除远程分支

## 变更记录

仓库内保留 [CHANGELOG.md](./CHANGELOG.md) 作为人工变更记录，按日期记录每次主要修改点。
后续每次较明显的调整，建议先更新记录，再提交代码。
记录格式建议按“日期 / 分支 / 修改点 / 结果”维护。

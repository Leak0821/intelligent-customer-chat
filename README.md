# 智能客服项目

这是一个面向邮件场景的智能客服 Agent 项目仓库。

## 项目说明

- 当前以文档先行方式收敛第一阶段规范
- 第一阶段实现基线已收敛为 `Java + Spring Boot + Spring AI`
- 当前目标是先把“可审、可拆、可实现”的规范合同沉淀完整

## 重要提醒

- 本项目不用于生产环境
- 任何实现、接口和数据结构都可能随练习过程调整
- 后续会根据实际需要逐步补充功能

## 当前状态

- 已完成 Git 仓库初始化并关联 GitHub
- 已建立基础的版本管理流程
- 已初始化一版 AI 协作规范和文档骨架
- 已完成第一阶段主流程工程骨架，包含邮件接入、意图归一、上下文加载、业务事实查询、知识检索、回复草稿、审核决策等最小闭环链路
- 已接入 MySQL 持久化、Redis 上下文缓存、Elasticsearch 混合检索骨架、Nacos 运行时配置骨架、XXL-JOB 执行器骨架
- 已补充会话摘要压缩、摘要落库、Spring AI 向量嵌入接入点，以及 Elasticsearch 知识索引初始化入口
- 第一阶段 OpenSpec 与研究合同已进入“边实现边校正”阶段，后续重点转向真实业务适配和基础设施联调

## 文档入口

- [AGENTS.md](./AGENTS.md)：仓库级 Agent 执行合同
- [docs/agent-playbook.md](./docs/agent-playbook.md)：项目级最佳实践初稿
- [docs/ai-workflow.md](./docs/ai-workflow.md)：`oh-my-codex + superpowers + OpenSpec + gstack` 工作流标准
- [docs/engineering-standards.md](./docs/engineering-standards.md)：与技术栈无关的实现原则
- [docs/project-scope.md](./docs/project-scope.md)：第一阶段业务边界与范围定义
- [docs/repository-boundaries.md](./docs/repository-boundaries.md)：哪些内容可以入库，哪些只留本地
- [research/README.md](./research/README.md)：探索材料放置位置
- [research/minimal-email-agent-loop.md](./research/minimal-email-agent-loop.md)：邮件 agent 第一条最小闭环链路研究
- [research/first-stage-technical-decisions.md](./research/first-stage-technical-decisions.md)：第一阶段技术设计决策清单
- [research/first-stage-sub-intent-seed-list.md](./research/first-stage-sub-intent-seed-list.md)：第一阶段首批子意图种子清单
- [research/first-stage-runtime-stack-baseline.md](./research/first-stage-runtime-stack-baseline.md)：第一阶段运行时技术栈基线
- [research/first-stage-implementation-skeleton.md](./research/first-stage-implementation-skeleton.md)：第一阶段 Java/Spring AI 实现骨架设计
- [research/first-stage-docker-compose-plan.md](./research/first-stage-docker-compose-plan.md)：第一阶段本地 Docker 基础设施方案
- [research/first-stage-pom-baseline.md](./research/first-stage-pom-baseline.md)：第一阶段 `pom.xml` 依赖基线建议
- [openspec/changes/README.md](./openspec/changes/README.md)：进行中的正式变更合同
- [openspec/specs/README.md](./openspec/specs/README.md)：稳定规格沉淀位置

## 当前建议审阅入口

- [openspec/changes/email-agent-minimal-loop/review-guide.md](./openspec/changes/email-agent-minimal-loop/review-guide.md)：当前第一阶段规范的统一审阅入口
- [openspec/changes/email-agent-minimal-loop/spec.md](./openspec/changes/email-agent-minimal-loop/spec.md)：第一阶段正式规格
- [openspec/changes/email-agent-minimal-loop/design.md](./openspec/changes/email-agent-minimal-loop/design.md)：第一阶段设计约束与技术收敛方向
- [openspec/changes/email-agent-minimal-loop/tasks.md](./openspec/changes/email-agent-minimal-loop/tasks.md)：当前已收敛任务与建议实现切片
- [research/first-stage-implementation-skeleton.md](./research/first-stage-implementation-skeleton.md)：审查通过后进入编码前的实现骨架参考
- [research/first-stage-slice-1-coding-plan.md](./research/first-stage-slice-1-coding-plan.md)：切片 1 的具体编码起步计划
- [research/first-stage-runtime-stack-baseline.md](./research/first-stage-runtime-stack-baseline.md)：当前已锁定的运行时与基础设施组合
- [research/first-stage-docker-compose-plan.md](./research/first-stage-docker-compose-plan.md)：本地 MySQL / Redis / ES / Nacos / XXL-JOB 的 Docker 规划
- [research/first-stage-pom-baseline.md](./research/first-stage-pom-baseline.md)：开始写工程骨架前的依赖清单参考

## 后续计划

- 继续把第一阶段骨架替换为真实能力，包括模型调用、订单/物流查询、知识库灌库与邮件发送
- 打通 Docker 本地依赖与运行时配置联调，完成可重复启动的开发环境
- 逐步补齐人工审核策略、观测告警、重试补偿与回放机制

## 本地环境

如果需要在当前项目内启用 Codex 本地环境，请在仓库根目录执行：

```bash
source ./scripts/use-local-codex.sh
```

这只会影响当前 shell，会把 `CODEX_HOME` 指向本仓库内的 `.codex-home`。

如果要启动本地基础依赖，可在仓库根目录执行：

```bash
docker compose up -d mysql redis elasticsearch nacos
```

当前默认使用本仓库内的 `.mvn/settings.xml` 与 `.m2/repository`，执行测试可直接使用：

```bash
mvn test
```

如果要接入 OpenAI 兼容模型网关，可优先配置以下环境变量：

```bash
OPENAI_API_KEY=...
SPRING_AI_OPENAI_BASE_URL=https://api.openai.com
SPRING_AI_OPENAI_CHAT_MODEL=gpt-4o-mini
SPRING_AI_OPENAI_EMBEDDING_MODEL=text-embedding-3-small
APP_AI_CHAT_ENABLED=true
APP_KNOWLEDGE_EMBEDDING_ENABLED=true
```

如果是 `DeepSeek`、`Qwen` 或其他 OpenAI 兼容网关，可直接替换 `base-url` 和模型名。

如果要把回复链路切换到真实 SMTP 发信，可额外配置：

```bash
APP_MAIL_OUTBOUND_ENABLED=true
APP_MAIL_OUTBOUND_PROVIDER=smtp
APP_MAIL_OUTBOUND_FROM_ADDRESS=support@example.com
APP_MAIL_OUTBOUND_FROM_NAME=intelligent-customer-chat
APP_MAIL_OUTBOUND_HOST=smtp.example.com
APP_MAIL_OUTBOUND_PORT=587
APP_MAIL_OUTBOUND_USERNAME=mailer@example.com
APP_MAIL_OUTBOUND_PASSWORD=your-password
APP_MAIL_OUTBOUND_AUTH_ENABLED=true
APP_MAIL_OUTBOUND_STARTTLS_ENABLED=true
APP_MAIL_OUTBOUND_SSL_ENABLED=false
APP_MAIL_DISPATCH_RETRY_ENABLED=true
APP_MAIL_DISPATCH_RETRY_MAX_ATTEMPTS=3
APP_MAIL_DISPATCH_RETRY_INITIAL_DELAY_SECONDS=60
APP_MAIL_DISPATCH_RETRY_BACKOFF_MULTIPLIER=2
```

默认仍然是 `noop` 发件器，目的是先保留完整发送状态机，同时避免本地开发或演示时误发真实邮件。
发送失败后默认会进入“待重试”状态，并通过本地定时器或 `XXL-JOB` 补偿入口继续推进；如果重试次数耗尽，则改为人工跟进状态。

## 本地演示接口

- `POST /api/workflows/demo`：提交一封测试邮件，返回 `WorkflowRun`
- `POST /api/workflows/demo/analysis`：提交一封测试邮件，直接返回清洗、意图、路由、业务事实、知识检索、草稿、审核结论
- `POST /api/workflows/demo/replay`：提交一封测试邮件，直接返回完整回放
- `GET /api/workflows/{runId}/replay`：按 `runId` 查看完整链路
- `GET /api/workflows/by-message/{messageId}/replay`：按 `messageId` 查看最新链路
- `POST /api/workflows/{runId}/approve-send`：把草稿从待审核推进到可发送
- `POST /api/workflows/{runId}/reject-send`：驳回当前草稿发送，回到人工审核状态
- `POST /api/workflows/{runId}/dispatch`：通过 no-op 发件适配层模拟发送
- `GET /api/workflows/{runId}/dispatches`：查看该链路的发送记录
- `GET /api/workflows/{runId}/reviews`：查看该链路的人工审核记录
- `POST /api/workflows/{runId}/retry-dispatch`：手工触发一条待重试派发
- `POST /api/workflows/dispatches/retry-due`：批量执行已到期的发送补偿任务

当前回放视图除了事件、草稿、派发记录外，也会带上人工审核记录，便于说明：

- 谁批准了发送
- 谁驳回了发送
- 最近一次发送是人工批准触发、人工重试还是调度补偿触发

## 知识库管理接口

- `GET /api/knowledge/seeds`：查看内置知识样本摘要
- `POST /api/knowledge/index/seeds`：批量灌入内置知识样本
- `POST /api/knowledge/index/sample`：手工灌入单篇知识文档
- `GET /api/knowledge/search?q=...`：直接验证当前检索结果

## 分支管理记录

- 这类练习项目建议保留特性分支，方便回看每次修改
- 分支命名建议使用 `feat/`、`fix/`、`chore/` 前缀
- 当前仓库会按正常 Git flow 记录每次分支开发和合并过程
- 如果某个分支已经合并但还想保留历史，可以先不删除远程分支

## 变更记录

仓库内保留 [CHANGELOG.md](./CHANGELOG.md) 作为人工变更记录，按日期记录每次主要修改点。
后续每次较明显的调整，建议先更新记录，再提交代码。
记录格式建议按“日期 / 分支 / 修改点 / 结果”维护。

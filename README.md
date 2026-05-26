# 智能客服项目

这是一个面向邮件场景的智能客服 Agent 项目仓库。

## 项目说明

- 当前以文档先行方式收敛第一阶段规范
- 第一阶段实现基线已收敛为 `Java + Spring Boot + Spring AI`
- 当前目标是先把“可审、可拆、可实现”的规范合同沉淀完整
- 第一版默认按“中小公司先跑通主链路”的思路实现，不过早引入过重的实时编排和复杂调度体系

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
- 已将售后业务查询从纯硬编码桩数据升级为可维护的本地业务数据目录，支持订单、物流、售后策略的运行时查看与更新
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
docker compose up -d mysql redis elasticsearch nacos xxl-job-admin
```

如果只是先跑最小闭环，不需要马上启 `XXL-JOB`，也可以先不拉起 `xxl-job-admin`。
第一版默认仍然可以走本地定时器或手工接口推进。

如果想先用第一版的本地配置启动应用，可以加上 `local` profile：

```bash
mvn -Dspring-boot.run.profiles=local spring-boot:run
```

或者直接在 IDE 里把 `spring.profiles.active` 设成 `local`。

如果要验证 `XXL-JOB` 调度链路，建议额外注意这几件事：

- `docker compose` 首次启动时会自动初始化 `xxl_job` 库和默认管理员
- 默认管理后台地址是 `http://127.0.0.1:8088/xxl-job-admin`
- 默认登录账号是 `admin / 123456`
- 应用侧需要额外打开 `APP_XXL_ENABLED=true`
- 执行器默认 `appName` 是 `intelligent-customer-chat-executor`

仓库里还提供了一个最小的 Maven 缓存清理脚本，专门处理网络失败后残留的 `*.lastUpdated`：

```bash
./scripts/cleanup-maven-lastupdated.sh
```

如果要把运行时 prompt / intent / retrieval 配置切到本地 `Nacos`，仓库里已经带了脱敏样例配置和发布脚本：

```bash
./scripts/publish-nacos-runtime-config.sh
```

默认会把 [agent-prompts.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/nacos/runtime-config/agent-prompts.json)、[agent-intents.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/nacos/runtime-config/agent-intents.json)、[agent-retrieval.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/nacos/runtime-config/agent-retrieval.json) 发布到本地 `Nacos` 的 `DEFAULT_GROUP`。

如果你要切到 `Nacos` 运行时配置，可以再打开：

```bash
APP_NACOS_CONFIG_ENABLED=true
APP_NACOS_SERVER_ADDR=127.0.0.1:8848
APP_NACOS_GROUP=DEFAULT_GROUP
```

当前这条脚本默认面向“本地 auth 关闭的 Nacos 容器”。
如果后面把 `Nacos` 权限体系打开，再单独补带鉴权的发布方式。

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

## 邮件处理策略

第一版邮件链路默认不要求实时处理。

当前实现更偏向中小公司常见做法：

- 先把邮件收下来，落一条 `mail_receipt`
- 可以先入队，再由后续任务批量推进工作流
- 也保留一个 `poll-and-process` 组合入口，方便本地快速验证

这样做的原因很简单：

- 第一版更重视闭环跑通和可复盘，而不是追求毫秒级实时
- 如果后面业务测试发现流程需要调整，轻量任务化方案比重工作流平台更容易改
- 等真实接起来之后，再决定是否要拆成更细的异步任务和调度链

## 审核策略

第一版审核层不追求复杂评分模型，当前先采用轻量规则：

- 草稿本身已标记为追问或人工审核时，直接沿用
- 售后业务事实冲突、事实临时不可用、订单/物流查不到时，优先转人工审核
- 售前如果没有知识检索支撑，优先转人工审核，避免幻觉式推荐
- 只有场景、事实和知识信号都比较安全时，才保持 `DRAFT_READY`

## 本地演示接口

- `POST /api/workflows/demo`：提交一封测试邮件，返回 `WorkflowRun`
- `POST /api/workflows/demo/analysis`：提交一封测试邮件，直接返回清洗、意图、路由、业务事实、知识检索、草稿、审核结论
- `POST /api/workflows/demo/replay`：提交一封测试邮件，直接返回完整回放
- `GET /api/workflows/{runId}/replay`：按 `runId` 查看完整链路
- `GET /api/workflows/by-message/{messageId}/replay`：按 `messageId` 查看最新链路
- `GET /api/workflows/{runId}/evaluation`：按 `runId` 查看最小评估样本视图
- `GET /api/workflows/evaluations/recent`：查看最近一批工作流评估样本
  - 支持 `scene`
  - 支持 `subIntent`
  - 支持 `workflowStatus`
  - 支持 `draftStatus`
  - 支持 `riskFlag`
- `POST /api/workflows/{runId}/approve-send`：把草稿从待审核推进到可发送
- `POST /api/workflows/{runId}/reject-send`：驳回当前草稿发送，回到人工审核状态
- `POST /api/workflows/{runId}/revise-draft`：人工修订草稿，并可选择是否再次送审
- `POST /api/workflows/{runId}/dispatch`：通过 no-op 发件适配层模拟发送
- `GET /api/workflows/{runId}/dispatches`：查看该链路的发送记录
- `GET /api/workflows/{runId}/reviews`：查看该链路的人工审核记录
- `POST /api/workflows/{runId}/retry-dispatch`：手工触发一条待重试派发
- `POST /api/workflows/dispatches/retry-due`：批量执行已到期的发送补偿任务
- `POST /api/mail/poll`：拉取邮箱新邮件并入队
- `POST /api/mail/process-pending`：批量处理待执行邮件
- `POST /api/mail/poll-and-process`：一键完成“拉取 + 处理”，便于本地快速验证
- `POST /api/mail/receipts/{messageId}/requeue`：把失败或待调整的邮件重新放回待处理队列

当前回放视图除了事件、草稿、派发记录外，也会带上人工审核记录，便于说明：

- 谁批准了发送
- 谁驳回了发送
- 草稿被修订到了第几版、最后由谁改动
- 最近一次发送是人工批准触发、人工重试还是调度补偿触发

新增的评估样本视图会进一步把回放压缩成一份更适合复盘和面试讲解的摘要，重点回答：

- 这封邮件被识别成了什么场景和子意图
- 是否触发了业务事实查询与知识检索
- 当前草稿停在什么发送前状态
- 最近一次审核、驳回或发送异常是什么
- 当前有哪些风险标记，例如人工审核、追问、重试中、业务冲突

最近评估样本接口已经支持轻量筛选，适合第一版快速回答这类问题：

- 目前售后物流类案例有多少条
- 哪些案例还停留在 `FOLLOW_UP_NEEDED`
- 哪些案例命中了 `manual_review_required`
- 哪些案例当前存在发送重试风险

## 知识库管理接口

- `GET /api/knowledge/seeds`：查看内置知识样本摘要
- `POST /api/knowledge/index/seeds`：批量灌入内置知识样本
- `POST /api/knowledge/index/sample`：手工灌入单篇知识文档
- `GET /api/knowledge/search?q=...`：直接验证当前检索结果

## 业务数据管理接口

- `GET /api/business/orders`：查看当前本地订单目录
- `POST /api/business/orders`：新增或更新一条订单记录
- `GET /api/business/logistics`：查看当前本地物流目录
- `POST /api/business/logistics`：新增或更新一条物流轨迹记录
- `GET /api/business/policies`：查看当前本地售后策略目录
- `POST /api/business/policies`：新增或更新一条售后策略记录

当前这组接口的定位是“第一阶段可演示、可调试的本地业务事实源”：

- 用于替代纯硬编码查询结果，先把售后链路跑通
- 便于后续在不改应用编排层的前提下，替换成真实订单系统、物流系统、MCP 或 CLI 能力
- 也便于面试演示时现场修改订单、物流、策略数据，观察工作流回放如何变化

## 分支管理记录

- 这类练习项目建议保留特性分支，方便回看每次修改
- 分支命名建议使用 `feat/`、`fix/`、`chore/` 前缀
- 当前仓库会按正常 Git flow 记录每次分支开发和合并过程
- 如果某个分支已经合并但还想保留历史，可以先不删除远程分支

## 变更记录

仓库内保留 [CHANGELOG.md](./CHANGELOG.md) 作为人工变更记录，按日期记录每次主要修改点。
后续每次较明显的调整，建议先更新记录，再提交代码。
记录格式建议按“日期 / 分支 / 修改点 / 结果”维护。

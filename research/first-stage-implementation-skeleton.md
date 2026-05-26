# 第一阶段实现骨架设计

## 1. 目的

这份文档用于把已经确认的第一阶段规范，进一步收敛成一份可直接指导编码的实现骨架。

它不替代 OpenSpec 合同，而是回答：

- 第一版 Java 项目建议怎么分层
- 哪些接口应该先定义出来
- 哪些状态对象应该作为主链路骨架
- 第一版更适合单体还是多模块

## 2. 第一阶段推荐实现形态

第一阶段更推荐：

- 单仓库
- 单应用
- 单模块起步
- 清晰包分层

当前运行时前提：

- `JDK 17`
- `Maven`
- `Spring Boot 3.4.x`
- `Spring AI 1.1.7`
- `MyBatis + MyBatis-Plus`
- `MySQL / Redis / Elasticsearch / Nacos / XXL-JOB` 通过 Docker 基础设施配套

而不是一开始就拆：

- 多服务
- 多模块 Maven 聚合
- 过重的工作流平台

原因：

- 当前最重要的是把邮件客服主链路跑通
- 当前边界还在持续收敛，过早拆模块会增加改动成本
- 面试展示更看重主链路清晰度，而不是服务数量

## 3. 第一阶段建议包结构

建议以 `com.xxx.customerchat` 这类根包起步，内部按职责分层：

```text
app/
  mail/
  workflow/
  intent/
  context/
  business/
  knowledge/
  reply/
  review/
  observability/

domain/
  mail/
  workflow/
  intent/
  context/
  business/
  reply/

infrastructure/
  mail/
  ai/
  business/
  knowledge/
  persistence/
  observability/

interfaces/
  scheduler/
  admin/
```

分层含义：

- `app/`：应用服务和主链路编排
- `domain/`：核心对象、状态、规则枚举
- `infrastructure/`：IMAP、Spring AI、外部系统、存储等具体实现
- `interfaces/`：定时任务触发、后续后台接口、人工审核入口等

建议补充的基础设施子目录：

```text
infrastructure/
  mail/
  ai/
  business/
  knowledge/
  persistence/
  cache/
  config/
  scheduler/
  observability/
```

## 4. 第一阶段核心应用服务

建议优先定义以下应用服务：

- `MailIngestionService`
- `WorkflowRunService`
- `IntentNormalizationService`
- `IntentRoutingService`
- `ContextLoadingService`
- `BusinessFactService`
- `KnowledgeRetrieveService`
- `ReplyDraftService`
- `ReviewDecisionService`
- `WorkflowReplayService`

职责划分：

- `MailIngestionService`：负责从接入层拿到新邮件并触发主流程
- `WorkflowRunService`：负责推进显式状态主链路
- 其他服务各自只负责一个阶段结果，不直接包办整条链路

## 5. 第一阶段建议的核心接口

建议尽早把以下接口抽出来：

### 5.1 邮件接入

- `MailSourceAdapter`
- `MailDedupService`
- `MailCleaner`

### 5.2 AI 与意图层

- `IntentNormalizer`
- `IntentRouter`
- `LlmClient`
- `EmbeddingService`

### 5.3 上下文与业务数据

- `ContextLoader`
- `OrderQueryGateway`
- `LogisticsQueryGateway`
- `AfterSalesPolicyGateway`
- `ConversationMemoryStore`
- `ProfileStore`

### 5.4 知识与回复

- `KnowledgeRetriever`
- `ReplyPlanner`
- `ReplyDraftRepository`
- `HybridSearchService`
- `ChunkIndexWriter`

### 5.5 审核与观测

- `ReviewPolicyEngine`
- `WorkflowEventRecorder`
- `WorkflowReplayReader`

### 5.6 配置与运行时基础

- `PromptConfigService`
- `IntentConfigService`
- `RetrievalConfigService`
- `ContextCompressionService`

原则：

- 主流程依赖接口，不依赖具体 `Spring AI`、IMAP 或外部系统实现类
- 具体 provider 或 adapter 只放在 `infrastructure/`
- `Redis`、`Nacos`、`Elasticsearch`、`XXL-JOB` 也都应通过基础设施实现层接入

## 6. 第一阶段关键领域对象

建议优先沉淀以下领域对象：

- `InboundMail`
- `WorkflowRun`
- `IntentNormalizationResult`
- `IntentRoutingResult`
- `ContextPacket`
- `BusinessFactResult`
- `KnowledgeRetrieveResult`
- `ReplyDraft`
- `ReviewDecision`
- `WorkflowEvent`
- `ConversationSummary`
- `RetrievalQuery`
- `RetrievalResult`

这些对象的作用：

- 让每个阶段的输入输出稳定
- 降低后续 prompt 和 adapter 变动对主流程的冲击

## 7. 第一阶段状态主链路建议

主流程建议不要只靠方法嵌套串起来，而是有显式阶段枚举。

建议阶段枚举：

- `MAIL_RECEIVED`
- `MAIL_CLEANED`
- `INTENT_NORMALIZED`
- `INTENT_ROUTED`
- `CONTEXT_LOADED`
- `BUSINESS_FACTS_READY`
- `KNOWLEDGE_READY`
- `REPLY_DRAFTED`
- `REVIEWED`
- `COMPLETED`
- `BLOCKED`

说明：

- `COMPLETED` 不等于“已发送”
- 第一阶段可以把“已生成可审核草稿”视为一种完成

## 8. 第一阶段建议的持久化对象

即使第一版不做复杂数据库设计，也建议预留以下持久化对象概念：

- `workflow_runs`
- `workflow_events`
- `reply_drafts`
- `mail_threads`
- `conversation_summaries`
- `user_profiles`

其中最关键的是：

- `workflow_runs`：当前跑到哪一步
- `workflow_events`：每一步做了什么
- `reply_drafts`：输出草稿与状态

## 9. 第一阶段与 Spring AI 的结合方式

既然已经确定 `Spring AI` 作为主 AI 抽象，第一阶段建议这样接：

- `IntentNormalizer` 内部使用 `Spring AI` 做结构化输出
- `IntentRouter` 可使用 `Spring AI` 或规则增强
- `KnowledgeRetriever` 背后接 embedding 和检索能力
- `ReplyPlanner` 使用 `Spring AI` 基于 facts 和 knowledge 组织草稿

但业务层仍应只依赖内部接口，不直接把 `ChatClient`、provider 参数、prompt 细节扩散到各层。

与其他基础设施的结合方向：

- `Redis`：用于最近会话上下文、压缩摘要和幂等辅助键
- `Nacos`：用于 prompt、意图配置、检索参数和审核阈值
- `Elasticsearch`：用于 `BM25 + kNN + RRF` 混合检索
- `XXL-JOB`：用于拉邮、重试、索引更新和压缩任务

## 10. 第一阶段不建议现在就做的骨架动作

当前不建议一开始就做：

- 多模块拆分
- 复杂 DDD 战术设计
- 过早引入消息队列
- 过早引入 Temporal
- 为未来所有渠道做统一总线

这些都可能后续再做，但不应该成为第一阶段编码前提。

## 11. 建议的编码起步顺序

如果按当前实现切片进入编码，更推荐：

1. 先建领域对象和状态枚举
2. 再建接口层
3. 再建主流程骨架
4. 再补 IMAP adapter
5. 再补意图归一化和路由
6. 再补 Redis 上下文与 Nacos 配置层
7. 再补业务查询和 RAG
8. 最后补审核、状态落库、回放

## 12. 当前建议结论

第一阶段的最佳实现起手式，不是先追求“高级框架感”，而是先把下面四件事稳住：

- 主流程阶段清楚
- 接口边界清楚
- 领域对象清楚
- 状态与事件可追踪

这样后面真正开始写代码时，系统才不会重新回到“边写边想流程”的状态。

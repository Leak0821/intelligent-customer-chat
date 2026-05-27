# 面试演示 Walkthrough

这份文档面向第一版项目演示，重点不是把所有功能讲完，而是把“为什么这样设计、现在能跑到哪里、后续怎么扩展”讲清楚。

## 1. 推荐演示顺序

建议按下面顺序讲：

1. 项目目标
2. 第一版为什么不做过重架构
3. 本地基础设施怎么起
4. 主链路怎么跑
5. 典型售前 / 售后样例
6. 审核和发送闭环
7. 回放与评估
8. 后续扩展方向

## 2. 开场先说什么

推荐先说三点：

1. 这是一个面向邮件场景的智能客服 Agent，不是单纯的聊天 Demo
2. 第一版故意按“中小公司先跑通闭环”的思路实现，没有一上来做重实时编排
3. 当前重点是把收件、归一化、路由、事实查询、知识检索、草稿、审核、派发、回放这条主链打通

## 3. 演示前准备

先说明本地环境已经支持：

- `docker compose` 起 MySQL / Redis / Elasticsearch / Nacos / XXL-JOB
- `Nacos` 运行时配置样例可一键发布
- `preflight` 可检查配置缺口
- demo / replay / evaluation / dispatch 都有固定接口
- 内置 demo 样例目录，可直接列出并一键执行

## 4. 推荐演示的 10 个样例

样例文件在：

- [pre-sales-recommendation.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/demo-scenarios/pre-sales-recommendation.json)
- [pre-sales-comparison.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/demo-scenarios/pre-sales-comparison.json)
- [pre-sales-general-inquiry.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/demo-scenarios/pre-sales-general-inquiry.json)
- [pre-sales-shipping-stock.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/demo-scenarios/pre-sales-shipping-stock.json)
- [after-sales-order-status.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/demo-scenarios/after-sales-order-status.json)
- [after-sales-manual-review.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/demo-scenarios/after-sales-manual-review.json)
- [after-sales-logistics.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/demo-scenarios/after-sales-logistics.json)
- [after-sales-policy.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/demo-scenarios/after-sales-policy.json)
- [after-sales-missing-id.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/demo-scenarios/after-sales-missing-id.json)
- [system-blocked-demo.json](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/ops/demo-scenarios/system-blocked-demo.json)

它们分别对应：

1. 售前推荐
2. 售前对比说明
3. 售前基础功能咨询
4. 售前库存 / 发货咨询
5. 售后订单状态查询
6. 售后人工审核
7. 售后物流查询
8. 售后政策说明
9. 售后缺关键编号，触发追问
10. 系统阻断演示

同时也可以直接走内置目录接口：

```bash
curl http://127.0.0.1:8080/api/workflows/demo/scenarios
curl -X POST "http://127.0.0.1:8080/api/workflows/demo/scenarios/pre-sales-recommendation?mode=analysis"
curl -X POST "http://127.0.0.1:8080/api/workflows/demo/scenarios/after-sales-logistics?mode=replay"
curl -X POST "http://127.0.0.1:8080/api/workflows/demo/scenarios/after-sales-manual-review?mode=review_loop"
curl -X POST "http://127.0.0.1:8080/api/workflows/demo/scenarios/after-sales-policy?mode=validate"
curl "http://127.0.0.1:8080/api/workflows/evaluations/summary?limit=20"
```

目录接口现在除了基本标题和主题外，还会直接返回：

- `recommendedMode`
- `demoFocus`
- `expectedResultType`
- `businessEvidenceHint`
- `knowledgeEvidenceHint`

如果主要讲售后链路，建议先对照 [after-sales-demo-matrix.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/docs/after-sales-demo-matrix.md) 选样例。

## 5. 演示时怎么跑

### 5.1 批量看分析结果

```bash
./scripts/demo-batch.sh analysis
```

你可以重点说明：

- 系统不是直接生成回复，而是先做场景和子意图识别
- 现在 analysis 结果里可以直接看到启发式基线、最终归一化结果和是否被模型改写
- 还可以直接看到是模型生效还是启发式回退，以及为什么回退
- 也可以看到上下文压缩状态、持久化摘要和 RAG 检索 query
- 如果走混合检索，还能看到最终采用的融合策略和回来的片段编号
- 现在也能直接看出 business facts 的角色、来源系统，以及 knowledge 主要是在补什么
- 售后会优先看订单 / 物流等业务事实
- 售前会优先结合知识样本做推荐方向

### 5.2 批量看完整回放

```bash
./scripts/demo-batch.sh replay
```

这里重点讲：

- 每条样例都能看到事件流、草稿、审核和派发相关状态
- 不是黑盒聊天，而是可复盘的工作流
- `replay.evidence` 里现在能直接看到 facts / knowledge / reply 的角色分工
- blocked 样例可以单独说明“这是系统级阻断演示，不代表普通客户意图”

### 5.3 单独演示发送闭环

```bash
./scripts/send-lifecycle-smoke.sh
```

这里重点讲：

- 草稿生成不等于直接发送

### 5.4 单独演示审核反馈回流

```bash
./scripts/review-feedback-loop-smoke.sh
```

这里重点讲：

- 高风险售后不会直接放行，而是先进入人工审核
- 审核意见会留下 `REJECT_SEND`
- 人工改稿后会留下 `REVISE_DRAFT -> RESUBMIT_REVIEW`
- `evaluation` 里能直接看到 `reviewTimeline / reviewCount / revisionCount`
- 需要先经过审核节点
- 审核后再进入派发记录
- 派发失败还可以补偿重试

### 5.5 批量校验内置样例是否跑偏

```bash
./scripts/demo-scenario-validate.sh
```

这里重点讲：

- 这不是再跑一遍普通 demo，而是让内置样例按各自 `recommendedMode` 自检
- 如果后续改了意图识别、facts 查询或回复策略，这个脚本可以快速发现哪些样例预期被打破
- 对面试演示来说，这能证明项目不仅有样例，还在主动约束样例质量

### 5.6 用汇总视图讲“最近一批跑得怎么样”

```bash
curl "http://127.0.0.1:8080/api/workflows/evaluations/summary?limit=20"
curl "http://127.0.0.1:8080/api/workflows/evaluations/summary?limit=20&scene=AFTER_SALES"
curl "http://127.0.0.1:8080/api/workflows/evaluations/summary?limit=20&scene=AFTER_SALES&businessFactStatus=INSUFFICIENT_INPUT"
curl "http://127.0.0.1:8080/api/workflows/evaluations/recent?limit=20&knowledgeRetrievalSource=policy-catalog"
./scripts/evaluation-insights-smoke.sh
```

这里重点讲：

- 最近一批运行里，售前 / 售后各占多少
- 哪些子意图出现得最多
- `businessFactStatuses` 能直接看出这一批是缺编号、查无结果、冲突，还是 facts 本来就不需要
- `knowledgeRoles / knowledgeRetrievalSources` 能快速说明知识主要在补什么、来自哪一路检索
- `replyFallbackReasons` 能直接看出草稿阶段更多是在追问模板、人工审核模板，还是模型回退
- 模板回退、追问、人工审核出现了多少次
- 这个接口适合做“当前质量画像”，单条 `evaluation` 适合做个案复盘

## 6. 推荐话术

### 6.1 为什么不直接上复杂工作流引擎

推荐说法：

“第一版我故意没有先上重实时编排。因为真实业务还没完全接起来时，过早把系统做重，会让后面改方向更痛。当前我优先把状态、边界和闭环跑通，再保留后续接更复杂调度或多 agent 的空间。”

### 6.2 为什么要保留人工审核

推荐说法：

“邮件客服不是普通闲聊。尤其售后场景涉及物流、退款、政策承诺，如果业务事实不完整或者知识召回不足，就应该保留人工审核，而不是让模型直接承诺。”

### 6.3 为什么要有回放和评估

推荐说法：

“如果没有回放和评估，系统就只有结果，没有过程。后面一旦识别错、检索偏、审核策略不合理，就很难定位问题。现在这个项目把过程保留下来，是为了后续可复盘、可调优。”

## 7. 面试官常见追问怎么接

### 7.1 如果后面要接真实邮箱怎么办

回答方向：

- 已经有 `IMAP` 适配层和本地邮件接入说明
- 第一版先用 demo 和手工触发验证主链
- 后续再切到真实收件箱轮询

### 7.2 如果后面要接真实知识库怎么办

回答方向：

- 现在已经有 ES 混合检索骨架
- 支持静态知识样本和向量检索接入点
- 后续只需要替换数据源和索引灌库流程

### 7.3 如果后面要完全自动发信怎么办

回答方向：

- 现有状态机已经把审核和派发拆开
- 后续可以把部分低风险场景从人工审核切到自动放行
- 但不会推翻当前状态结构

## 8. 最后收口怎么讲

建议最后收口成一句话：

“这个项目当前不是停留在一个聊天 Demo，而是已经把邮件智能客服第一版最关键的工程骨架和本地联调链条搭起来了。后续继续接真实邮箱、真实模型、真实知识库和真实业务系统时，不需要推翻主链路。”

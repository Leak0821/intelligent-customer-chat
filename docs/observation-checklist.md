# 关键观察清单

这份清单用于本地联调、演示或后续接真实系统时，快速判断哪里正常、哪里异常。

## 1. 先看配置和基础设施

优先检查：

1. `GET /api/runtime-config/preflight`
2. `GET /api/runtime-config`
3. `docker compose ps`

重点观察：

- `ready` 是否为 `true`
- 哪些 feature 是 `OK / WARN / ERROR / SKIPPED`
- 当前运行时配置来源是 `local-default` 还是 `nacos`

## 2. 再看业务与知识样本

优先检查：

1. `GET /api/business/orders`
2. `GET /api/business/logistics`
3. `GET /api/business/policies`
4. `GET /api/knowledge/seeds`

重点观察：

- 本地订单和物流目录是否有样本
- 售后策略是否可读
- 知识种子是否覆盖售前 / 售后

## 3. 再看工作流主链

优先检查：

1. `POST /api/workflows/demo`
2. `POST /api/workflows/demo/analysis`
3. `GET /api/workflows/{runId}/replay`
4. `GET /api/workflows/{runId}/evaluation`

重点观察：

- scene 是否符合预期
- subIntent 是否符合预期
- heuristicBaseline 和 normalizationResult 是否有差异
- normalizationSource 是 `heuristic_fallback` 还是 `llm_with_guardrails`
- fallbackReason 是否说明了 `llm_unavailable` 或 `llm_response_invalid`
- guardrailActions 是否命中了订单号等关键约束
- contextDiagnostics 里是否能看到消息计数、压缩阈值和持久化摘要
- compressionDecision / compressionSkipReason 是否解释了为什么压缩或为什么跳过
- summaryResolutionSource 是否说明了摘要来自缓存、持久化摘要还是兜底拼装
- knowledgeDiagnostics 里是否能看到 retrievalQuery，以及 bm25 / vector 两路召回
- retrievalSource / fusionStrategy 是否说明了最终采用的检索通路和融合方式
- fusedSnippetIds 是否和最终 knowledge 结果对得上
- 是否触发了 business facts
- 是否触发了 knowledge
- draft 状态和 send readiness 是否合理

## 4. 如果是售后场景，重点看什么

重点看：

- 是否识别到订单号 / 物流号
- 是否命中本地订单和物流目录
- 如果没识别到关键编号，是否触发追问
- 如果事实不足或冲突，是否走人工审核

## 5. 如果是售前场景，重点看什么

重点看：

- 是否识别成推荐 / 对比 / 库存发货类意图
- 是否有知识样本支撑
- 如果知识不足，是否走人工审核而不是硬答

## 6. 如果要看发送闭环，重点看什么

优先检查：

1. `POST /api/workflows/{runId}/approve-send`
2. `POST /api/workflows/{runId}/dispatch`
3. `GET /api/workflows/{runId}/dispatches`
4. `GET /api/workflows/{runId}/reviews`

重点观察：

- 草稿是否先经过审核再发送
- 派发记录是否成功落库
- 审核记录里是否有 reviewer 和 note
- 如果 provider 是 `noop`，不要误以为已经真实外发

## 7. 如果要看异常和风险，重点看什么

优先检查：

1. `GET /api/workflows/evaluations/recent`
2. `GET /api/workflows/{runId}/evaluation`

重点观察：

- 是否有 `manual_review_required`
- 是否有 `follow_up_needed`
- 是否有 `retry_pending`
- 是否有 `business_conflict`

## 8. 快速判断一句话

如果下面这几件事同时成立，说明第一版本地闭环基本健康：

- `preflight` 正常
- demo 能生成 run
- analysis 能给出 scene / subIntent
- replay 能看到草稿和事件
- evaluation 能看到风险标记
- approve-send / dispatch / reviews / dispatches 能串起来

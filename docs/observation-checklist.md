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
- heuristicMatchedSignals 是否能解释启发式到底命中了哪些规则
- contextDiagnostics 里是否能看到消息计数、压缩阈值和持久化摘要
- compressionDecision / compressionSkipReason 是否解释了为什么压缩或为什么跳过
- summaryResolutionSource 是否说明了摘要来自缓存、持久化摘要还是兜底拼装
- businessFactDiagnostics 里是否能直接看出 facts 的角色、来源系统、缺口和冲突标记
- knowledgeDiagnostics 里是否能看到 retrievalQuery，以及 bm25 / vector 两路召回
- retrievalQuery 是否已经把主问题、场景意图、上下文摘要和关键 facts 提示组装进去，而不是直接拿整段原邮件生搜
- retrievalSource / fusionStrategy 是否说明了最终采用的检索通路和融合方式
- knowledgeRole / factsFirstApplied 是否说明了知识是在补充什么，以及是否真的受 facts 约束
- factGroundingSignals 是否能看出知识片段有没有和 facts / filters 形成有效衔接
- fusedSnippetIds 是否和最终 knowledge 结果对得上
- 是否触发了 business facts
- 是否触发了 knowledge
- replyDiagnostics 里是否能看到 replySource、llmAttempted、fallbackReason
- replyDiagnostics.factPreview / knowledgeSnippetIds 是否与前面的 facts 和知识召回对得上
- evaluation 里是否也能看到 replySource / replyFallbackReason，而不是只能在 analysis 里看
- replay / evaluation 里是否也能直接说明 facts 与 knowledge 的角色分工，而不是只剩原始事件摘要
- evaluation summary 里是否能直接看到 `businessFactStatuses / knowledgeRoles / knowledgeRetrievalSources / replyFallbackReasons`
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
2. `GET /api/workflows/evaluations/summary`
3. `GET /api/workflows/{runId}/evaluation`
4. `POST /api/workflows/demo/scenarios/after-sales-missing-id?mode=analysis`
5. `POST /api/workflows/demo/scenarios/after-sales-manual-review?mode=analysis`
6. `POST /api/workflows/demo/scenarios/system-blocked-demo?mode=replay`

重点观察：

- 是否有 `manual_review_required`
- 是否有 `follow_up_needed`
- 是否有 `retry_pending`
- 是否有 `business_conflict`
- `summary` 里能否直接看出最近一批运行中，售前 / 售后各有多少条
- 是否能按 `businessFactStatus / knowledgeRetrievalSource / replyFallbackReason` 继续缩小问题范围
- 是否能按 `businessFactRole / knowledgeRole` 直接聚焦“facts 在扮演什么角色”“knowledge 在补什么”
- `summary` 里能否直接看出模板回退、人工审核、追问分别出现了多少次
- evaluation 里是否能看到 `reviewCount / revisionCount / resubmittedForReview`
- reviewTimeline 是否能串起 `REVISE_DRAFT -> RESUBMIT_REVIEW -> APPROVE_SEND / REJECT_SEND`
- blocked 样例的 `run.status` 是否为 `BLOCKED`

## 8. 快速判断一句话

如果下面这几件事同时成立，说明第一版本地闭环基本健康：

- `preflight` 正常
- demo 能生成 run
- analysis 能给出 scene / subIntent
- replay 能看到草稿和事件
- evaluation 能看到风险标记
- approve-send / dispatch / reviews / dispatches 能串起来

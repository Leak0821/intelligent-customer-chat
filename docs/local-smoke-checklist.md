# 本地 Smoke 清单

这份清单用于第一版本地联调和演示，不追求完整压测，只追求“能按固定顺序跑通”。

## 1. 启动顺序

1. 启动基础设施

```bash
docker compose up -d mysql redis elasticsearch nacos xxl-job-admin
```

2. 发布本地运行时配置到 Nacos

```bash
./scripts/publish-nacos-runtime-config.sh
```

3. 启动应用

```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

## 2. 启动后先看什么

1. 先看运行前体检

```bash
curl http://127.0.0.1:8080/api/runtime-config/preflight
```

2. 再看运行时配置

```bash
curl http://127.0.0.1:8080/api/runtime-config
```

3. 确认业务样本可读

```bash
curl http://127.0.0.1:8080/api/business/orders
curl http://127.0.0.1:8080/api/business/logistics
curl http://127.0.0.1:8080/api/business/policies
```

4. 确认知识样本可读

```bash
curl http://127.0.0.1:8080/api/knowledge/seeds
```

5. 如果要看邮件接入控制面

```bash
curl http://127.0.0.1:8080/api/mail/overview
```

6. 如果要单独验证业务事实查询

```bash
curl http://127.0.0.1:8080/api/business/orders/ABCD1234
curl http://127.0.0.1:8080/api/business/logistics/ZXCV9876
curl http://127.0.0.1:8080/api/business/policies/by-intent/logistics_tracking
curl -X POST http://127.0.0.1:8080/api/business/facts/preview \
  -H 'Content-Type: application/json' \
  -d '{"customerEmail":"buyer@example.com","scene":"AFTER_SALES","subIntent":"logistics_tracking","orderId":"ABCD1234","trackingNumber":"ZXCV9876","queryReason":"manual smoke preview"}'
```

## 3. 最小闭环验证

### 3.1 售后样例

1. 提交 demo 邮件

```bash
curl -sS -X POST http://127.0.0.1:8080/api/workflows/demo \
  -H 'Content-Type: application/json' \
  -d '{"messageId":"smoke-after-sales-001","threadId":"smoke-thread-001","from":"customer@example.com","subject":"Need help with tracking","body":"Hi, my order ABCD1234 tracking number is ZXCV9876. Could you check the latest logistics status?"}'
```

2. 直接看分析结果

```bash
curl -sS -X POST http://127.0.0.1:8080/api/workflows/demo/analysis \
  -H 'Content-Type: application/json' \
  -d '{"messageId":"smoke-after-sales-001","threadId":"smoke-thread-001","from":"customer@example.com","subject":"Need help with tracking","body":"Hi, my order ABCD1234 tracking number is ZXCV9876. Could you check the latest logistics status?"}'
```

分析结果里建议重点确认：

- `intentDiagnostics.heuristicBaseline`
- `intentDiagnostics.normalizationChangedByModel`
- `intentDiagnostics.normalizationSource`
- `intentDiagnostics.fallbackReason`
- `intentDiagnostics.guardrailActions`
- `contextDiagnostics.totalMessageCount`
- `contextDiagnostics.latestPersistedSummary`
- `contextDiagnostics.compressionDecision`
- `contextDiagnostics.compressionSkipReason`
- `contextDiagnostics.summaryResolutionSource`
- `knowledgeDiagnostics.retrievalQuery`
- `knowledgeDiagnostics.retrievalSource`
- `knowledgeDiagnostics.fusionStrategy`
- `knowledgeDiagnostics.fusedSnippetIds`
- `knowledgeDiagnostics.bm25Snippets`
- `knowledgeDiagnostics.vectorSnippets`
- `replyDiagnostics.replySource`
- `replyDiagnostics.llmAttempted`
- `replyDiagnostics.llmResponseAccepted`
- `replyDiagnostics.fallbackReason`
- `replyDiagnostics.factPreview`
- `replyDiagnostics.knowledgeSnippetIds`
- `evaluation.replySource`
- `evaluation.replyFallbackReason`

3. 如果需要完整回放，继续查看

```bash
curl http://127.0.0.1:8080/api/workflows/<runId>/replay
curl http://127.0.0.1:8080/api/workflows/<runId>/evaluation
curl http://127.0.0.1:8080/api/workflows/<runId>/events
curl http://127.0.0.1:8080/api/workflows/<runId>/draft
```

4. 如果要验证发送闭环，再执行

```bash
curl -sS -X POST http://127.0.0.1:8080/api/workflows/<runId>/approve-send \
  -H 'Content-Type: application/json' \
  -d '{"reviewer":"smoke-bot","approvalNote":"approved by smoke checklist"}'
curl -sS -X POST http://127.0.0.1:8080/api/workflows/<runId>/dispatch
curl http://127.0.0.1:8080/api/workflows/<runId>/dispatches
curl http://127.0.0.1:8080/api/workflows/<runId>/reviews
```

补充观察：

- `evaluation.reviewCount`
- `evaluation.revisionCount`
- `evaluation.resubmittedForReview`
- `evaluation.reviewTimeline`

### 3.2 售前样例

1. 提交 demo 邮件

```bash
curl -sS -X POST http://127.0.0.1:8080/api/workflows/demo \
  -H 'Content-Type: application/json' \
  -d '{"messageId":"smoke-pre-sales-001","threadId":"smoke-thread-002","from":"customer@example.com","subject":"Need recommendation","body":"Hi, I am looking for a product recommendation for my living room."}'
```

2. 看分析和回放

```bash
curl -sS -X POST http://127.0.0.1:8080/api/workflows/demo/replay \
  -H 'Content-Type: application/json' \
  -d '{"messageId":"smoke-pre-sales-001","threadId":"smoke-thread-002","from":"customer@example.com","subject":"Need recommendation","body":"Hi, I am looking for a product recommendation for my living room."}'
```

## 4. 判断通过的标准

- `preflight` 返回 `ready=true`
- `demo` 能生成 `WorkflowRun`
- `analysis` 能解释回复阶段到底走了 `llm`、模板兜底还是追问模板
- `analysis` 能返回 scene / subIntent / facts / knowledge / draft
- `replay` 能看到事件、草稿、审核或派发信息
- `knowledge` 和 `business` 接口能返回可读样本
- 发送闭环可通过 `approve-send -> dispatch -> dispatches -> reviews` 跑通

## 5. 如果现场想快速排错

- 先看 `preflight`
- 再看 `mail/overview`
- 再看 `runtime-config`
- 再看 `business` 和 `knowledge`
- 最后看 `workflow/demo`

## 6. 附加脚本

如果要快速验证邮件接入控制面，可执行：

```bash
./scripts/mail-ops-smoke.sh
```

如果要快速验证业务事实链路，可执行：

```bash
./scripts/business-facts-smoke.sh
```

如果要验证“邮件先入队，稍后再处理”的异步链路，可执行：

```bash
./scripts/async-mail-smoke.sh
```

如果要验证待审核/待发送队列视图，可执行：

```bash
./scripts/workflow-queues-smoke.sh
```

如果要快速验证“追问 / 人工审核 / 阻断”三类兜底路径，可执行：

```bash
./scripts/fallback-paths-smoke.sh
```

如果要演示“人工审核拒绝 -> 修改草稿 -> 重提审核 -> 放行”的反馈闭环，可执行：

```bash
./scripts/review-feedback-loop-smoke.sh
```

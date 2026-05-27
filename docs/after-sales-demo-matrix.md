# 售后演示矩阵

这份文档只服务当前第一版 demo，不追求覆盖全部售后意图，而是把最值得讲的高频样例先固定下来。

## 1. 推荐优先演示的 5 个售后样例

| 场景 ID | 推荐 mode | 预期输出类型 | 这条样例主要想证明什么 | facts 重点看什么 | knowledge 重点看什么 |
| --- | --- | --- | --- | --- | --- |
| `after-sales-order-status` | `replay` | 直接草稿 | 订单状态查询是典型的 facts-first 场景 | 是否命中订单状态、发货阶段、实体解析 | 是否只补状态解释和时效预期，而不是替代事实 |
| `after-sales-logistics` | `replay` | 直接草稿 | 物流查询里 facts 与 knowledge 如何协同 | 订单 / 物流 facts 是否成功、冲突或无结果 | 是否补物流节点解释、运输预期和保守说明 |
| `after-sales-policy` | `analysis` | 直接草稿 | 政策说明不是纯知识问答，而是要先看订单上下文是否已确认 | 是否先命中订单 facts，再决定如何解释政策边界 | 是否补政策边界、处理步骤和风险保守表达 |
| `after-sales-missing-id` | `analysis` | 先追问 | 缺编号时系统要敢于停下来追问 | 是否明确给出 `INSUFFICIENT_INPUT` | 是否只补通用说明，而不冒充查到了订单结果 |
| `after-sales-manual-review` | `review_loop` | 人工审核 | 高风险赔付 / 退款诉求要进入审核闭环 | 是否先核验订单上下文，再因为高风险诉求进入人工审核 | 是否只补政策措辞，不直接做赔付承诺 |

## 2. 面试或演示时建议怎么讲

推荐顺序：

1. 先跑 `after-sales-order-status`
2. 再跑 `after-sales-logistics`
3. 再跑 `after-sales-policy`
4. 然后展示 `after-sales-missing-id`
5. 最后再讲 `after-sales-manual-review`

这样排序的原因：

- 先讲最标准的 facts-first 场景，容易建立认知
- 再讲 facts 和 knowledge 会出现协同甚至冲突的场景
- 再讲政策说明这种“不是只靠 facts，也不是只靠知识”的中间地带
- 最后再展示追问和人工审核，让兜底路径显得自然

## 3. 每条样例至少看哪几个字段

所有售后样例都建议至少看：

- `intentDiagnostics.heuristicBaseline`
- `intentDiagnostics.guardrailActions`
- `businessFactDiagnostics.factStatus`
- `businessFactDiagnostics.factRole`
- `businessFactDiagnostics.sourceSystems`
- `knowledgeDiagnostics.knowledgeRole`
- `knowledgeDiagnostics.retrievalSource`
- `replyDiagnostics.replySource`
- `replyDiagnostics.fallbackReason`

如果走 `replay / evaluation`，再重点看：

- `replay.evidence.businessFactRole`
- `replay.evidence.knowledgeRole`
- `evaluation.businessFactRole`
- `evaluation.knowledgeRole`
- `evaluation.replySource`
- `evaluation.replyFallbackReason`

## 4. 当前版本不要讲太满的点

当前第一版不要把下面这些说得过重：

- 不要把物流样例讲成“已经接了真实物流系统”
- 不要把政策样例讲成“已经具备完整法务级政策引擎”
- 不要把人工审核样例讲成“已经有完整工单平台”

当前更准确的说法是：

- 主链路已经具备 facts、knowledge、审核、回放、评估这些关键骨架
- 第一版重点是把可解释、可演示、可复盘的闭环做出来
- 后续再逐步接真实邮箱、真实知识库和真实业务系统

# 第一阶段回复草稿状态模型

## 1. 目的

这份文档用于定义第一阶段回复草稿从生成到发送前的最小状态模型。

目标是保证：

- 草稿是可追踪对象
- 每个阶段状态可解释
- 后续无论走人工审核还是自动发送，都不用推翻状态结构

## 2. 第一阶段不应只有“有草稿 / 无草稿”

真实邮件客服里，草稿不是一个简单字符串。

第一阶段至少应能区分：

- 草稿是否已生成
- 草稿是否需要补充信息
- 草稿是否需要人工审核
- 草稿是否因系统问题暂时不能推进

## 3. 建议的主状态

第一阶段建议至少保留以下主状态：

1. `draft_ready`
2. `follow_up_needed`
3. `human_review_required`
4. `blocked`

状态说明：

- `draft_ready`：已生成可继续处理的回复草稿
- `follow_up_needed`：当前应先向客户追问补充信息
- `human_review_required`：当前草稿或问题需要人工审核或人工接管
- `blocked`：由于系统异常、数据冲突或其他阻塞原因，当前不能继续自动推进

## 4. 建议的附加状态字段

除主状态外，建议保留以下字段：

- `draft_version`
- `status_reason`
- `required_follow_up_fields`
- `review_required_reason`
- `blocking_reason`
- `next_action`

字段说明：

- `draft_version`：草稿版本号，便于后续修改和回放
- `status_reason`：当前状态原因摘要
- `required_follow_up_fields`：需要客户补充的关键字段
- `review_required_reason`：触发人工审核的原因
- `blocking_reason`：系统阻塞原因
- `next_action`：下一步建议动作

## 5. 发送前状态建议

由于第一阶段暂不锁定最终发送策略，建议把发送前状态与草稿状态分开。

建议额外保留：

- `send_readiness`

取值建议：

- `not_applicable`
- `pending_review`
- `ready_for_send`
- `hold`

说明：

- `not_applicable`：当前还不处于可发送阶段，例如先追问
- `pending_review`：已生成草稿，但等待人工审核
- `ready_for_send`：从状态上看已具备发送条件
- `hold`：暂不允许发送

## 6. 状态迁移建议

第一阶段推荐的典型迁移：

1. `draft_ready -> pending_review`
2. `draft_ready -> ready_for_send`
3. `follow_up_needed -> not_applicable`
4. `human_review_required -> pending_review`
5. `blocked -> hold`

需要注意：

- `blocked` 不应被误当成“无结果”
- `follow_up_needed` 不应被误当成“失败”

## 7. 第一阶段建议结论

第一阶段的回复状态模型至少要体现：

- 草稿已就绪
- 需要追问
- 需要人工审核
- 被阻塞不能推进

这样后续无论做后台审核、发件箱草稿还是自动发送开关，都有稳定状态基础。

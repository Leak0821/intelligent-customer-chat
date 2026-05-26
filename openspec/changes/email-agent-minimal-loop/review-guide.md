# 审阅指南：邮件 Agent 第一阶段最小闭环

## 1. 目的

这份文档用于给当前这组第一阶段规范提供一个统一审阅入口。

它回答三个问题：

- 现在这份变更处于什么阶段
- 建议按什么顺序阅读
- 审阅时应该重点挑什么问题

## 2. 当前阶段

按当前仓库工作流，这组文档已经从“范围澄清”和“初步研究”推进到：

- `OpenSpec review-spec / design` 已基本收敛

当前状态不是开始写代码，而是：

- 已形成第一阶段正式合同草案
- 已补齐关键技术与行为边界
- 已具备进入一次 `gstack` 审设计的条件

## 3. 建议阅读顺序

如果是第一次审这组文档，建议按下面顺序阅读：

1. [docs/project-scope.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/docs/project-scope.md)
2. [openspec/changes/email-agent-minimal-loop/proposal.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/openspec/changes/email-agent-minimal-loop/proposal.md)
3. [openspec/changes/email-agent-minimal-loop/spec.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/openspec/changes/email-agent-minimal-loop/spec.md)
4. [openspec/changes/email-agent-minimal-loop/design.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/openspec/changes/email-agent-minimal-loop/design.md)
5. [openspec/changes/email-agent-minimal-loop/tasks.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/openspec/changes/email-agent-minimal-loop/tasks.md)

然后按主题补读研究合同：

6. [research/first-stage-technical-decisions.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-technical-decisions.md)
7. [research/first-stage-runtime-stack-baseline.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-runtime-stack-baseline.md)
8. [research/first-stage-sub-intent-seed-list.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-sub-intent-seed-list.md)
9. [research/first-stage-intent-normalization-contract.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-intent-normalization-contract.md)
10. [research/first-stage-context-loading-contract.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-context-loading-contract.md)
11. [research/first-stage-business-data-access-contract.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-business-data-access-contract.md)
12. [research/first-stage-rag-boundary.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-rag-boundary.md)
13. [research/first-stage-pre-sales-branch.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-pre-sales-branch.md)
14. [research/first-stage-after-sales-branch.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-after-sales-branch.md)
15. [research/first-stage-business-data-rag-coordination.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-business-data-rag-coordination.md)
16. [research/first-stage-review-and-fallback.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-review-and-fallback.md)
17. [research/first-stage-reply-state-model.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-reply-state-model.md)
18. [research/first-stage-observability-replay.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-observability-replay.md)

如果当前审查通过，进入编码前建议继续看：

19. [research/first-stage-implementation-skeleton.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-implementation-skeleton.md)
20. [research/first-stage-slice-1-coding-plan.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/research/first-stage-slice-1-coding-plan.md)

## 4. 当前已确认的第一阶段基线

当前已经固定的核心基线如下：

- 邮件入口：`IMAP`
- 运行时基线：`JDK 17 + Maven + Spring Boot 3.4.x`
- 主流程：`Java + Spring Boot + 显式状态主链路`
- AI 抽象：`Spring AI 1.1.7`
- 阿里模型生态：后续按需通过 `Spring AI Alibaba` 补 provider 接入
- 基础设施：`MySQL 8.0.x + Redis 7.x + Elasticsearch 8.18.x + Nacos + XXL-JOB + Docker Compose`
- 主场景：售前、售后
- 首批子意图：售前 4 个、售后 4 个
- 输出状态：直接草稿、先追问、转人工
- 回复主状态：`draft_ready / follow_up_needed / human_review_required / blocked`

## 5. 审阅时建议重点挑的问题

这轮审查不需要再回到大范围头脑风暴，更适合重点检查：

- `scope` 是否仍然过大或仍然模糊
- `spec` 和 `design` 是否存在互相矛盾
- 某些边界是否仍然过度依赖“到时候再看”
- 是否有把 RAG 当真值源的风险
- 是否有把主流程绑死在单一 adapter / provider 的风险
- 审核、追问、转人工边界是否足够明确
- 状态模型和实现切片是否足以支撑真正开工

## 6. 当前有意保留、但未锁死的事项

以下事项是当前有意后置，而不是遗漏：

- 最终是否自动发送邮件
- 人工审核界面的具体形态
- 最终向量库和检索优化方案
- 外部业务系统最终采用 MCP、API、skill 还是 CLI
- 第一阶段之后的完整意图树扩展方式
- 长期记忆、复杂画像或多渠道扩展

## 7. 当前审阅结论

到当前为止，这组文档已经不只是“项目想法”。

它已经具备：

- 范围合同
- OpenSpec 合同
- 技术决策基线
- 行为合同
- 状态与兜底模型
- 可执行实现切片

因此当前最合理的下一步是：

- 进入一次正式设计审查
- 根据审查意见做最后一轮收敛
- 然后按实现骨架设计进入编码

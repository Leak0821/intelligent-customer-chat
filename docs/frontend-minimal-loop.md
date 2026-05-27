# 前端最小闭环接入

这份文档只回答一个问题：

- 如果现在要让前端先把邮件智能客服流程跑通，最小应该接哪些接口

当前建议不要一开始就接所有管理接口，而是先围绕一条最小闭环完成联调。

如果只是先看一版已经接好的页面，启动应用后直接打开：

```text
http://127.0.0.1:8080/
```

仓库已经内置了一个最小静态控制台，直接消费下文这些接口。

## 1. 当前是否已有完整小闭环

有。

当前后端已经具备下面这条最小闭环：

1. 前端提交一封邮件内容
2. 后端完成清洗、意图归一、场景路由、上下文加载、业务 facts 查询、知识检索、回复草稿生成
3. 后端给出当前状态、风险结论、草稿内容和下一步动作
4. 前端按状态决定是展示追问草稿、进入人工审核，还是执行发送
5. 后端保留回放、评估、审核和派发记录

注意：

- 这是“后端工作流闭环”
- 还不是“真实邮箱 + 真实订单系统 + 真实知识库 + 真实自动发信”全部接通的生产闭环

## 2. 前端第一版只接哪些接口

第一版建议只接下面 7 个接口。

### 2.1 创建并直接拿到当前 case

```bash
POST /api/workflows/demo/case
```

用途：

- 提交测试邮件
- 直接返回当前这封邮件的收敛结果

前端最先接这个接口即可。

### 2.2 刷新查看当前 case

```bash
GET /api/workflows/{runId}/case
```

用途：

- 刷新当前邮件处理状态
- 获取草稿、风险、可操作动作

这个接口是当前最推荐给前端直接消费的主接口。

### 2.3 运行内置场景并直接拿到 case

```bash
POST /api/workflows/demo/scenarios/{scenarioId}/case
```

用途：

- 直接运行仓库里的典型售前 / 售后样例
- 省掉前端自己组织测试正文
- 适合演示和回归联调

### 2.4 审核通过

```bash
POST /api/workflows/{runId}/approve-send
```

用途：

- 人工确认草稿可发送

### 2.5 审核拒绝

```bash
POST /api/workflows/{runId}/reject-send
```

用途：

- 人工拒绝当前草稿

### 2.6 改稿

```bash
POST /api/workflows/{runId}/revise-draft
```

用途：

- 人工修改标题和正文
- 可选择是否重新提交审核

### 2.7 执行发送

```bash
POST /api/workflows/{runId}/dispatch
```

用途：

- 把已放行草稿推进到发送链路

## 3. `case` 接口返回什么

`case` 视图已经收敛了前端最常用的信息：

- 基本标识：`runId / messageId / threadId`
- 客户信息：`sender / subject`
- 意图结果：`scene / subIntent`
- 当前流程状态：`workflowStatus / workflowStage / workflowReason`
- 草稿状态：`draftStatus / sendReadiness / nextAction`
- 风险结论：`riskDecision`
- 前端当前可用动作：`availableActions`
- 一句话摘要：`summaryMessage`
- 证据信息：`normalizationSummary / businessFactsSummary / knowledgeSummary`
- 草稿内容：`draftSubject / draftBody / draftVersion`

这意味着前端第一版不需要自己拼：

- `replay`
- `evaluation`
- `draft`
- `review`
- `dispatch`

至少在主页面展示上，不需要拼这些底层接口。

## 4. 推荐的前端页面流程

第一版建议只做一个页面闭环：

1. 填写客户邮件主题和正文
2. 调用 `POST /api/workflows/demo/case`
3. 展示 `summaryMessage`
4. 展示草稿正文
5. 展示 `riskDecision`
6. 根据 `availableActions` 决定展示哪些按钮

按钮建议：

- 有 `APPROVE_SEND` 时显示“审核通过”
- 有 `REJECT_SEND` 时显示“驳回”
- 有 `REVISE_DRAFT` 时显示“改稿”
- 有 `DISPATCH` 时显示“发送”
- 有 `RETRY_DISPATCH` 时显示“重试发送”

## 5. 当前明确不建议前端先做的事

第一版先不要急着做：

- 实时收件箱轮询
- 长轮询或 WebSocket 状态推送
- 多渠道统一收口
- 复杂工作台
- 大而全的运营后台

先把下面这件事做成：

- 一封测试邮件进来后，前端能完整看到“识别结果 -> 草稿 -> 审核/发送动作 -> 最终状态”

这才是当前第一阶段真正需要证明的闭环。

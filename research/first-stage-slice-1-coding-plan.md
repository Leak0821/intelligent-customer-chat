# 第一阶段切片 1 编码计划

## 1. 目的

这份文档用于把 [tasks.md](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/openspec/changes/email-agent-minimal-loop/tasks.md) 里的“切片 1：邮件接入与基础主链路骨架”进一步细化成可直接开工的编码计划。

这一刀不追求功能完整，而是追求：

- 项目能启动
- 主状态能跑
- 邮件接入和主链路有最小骨架
- 后续切片能稳定往里填能力

## 2. 切片 1 的明确目标

切片 1 完成时，系统至少应做到：

1. 有一个可启动的 `Spring Boot` 应用
2. 有显式主链路状态枚举
3. 有邮件接入 adapter 接口与 `IMAP` 实现占位
4. 有 `workflow_run` 和 `workflow_event` 的最小持久化概念
5. 能把一封邮件从“接入”推进到“主链路空跑完成”

注意：

- 切片 1 不要求真实意图识别
- 不要求真实 RAG
- 不要求真实业务查询
- 不要求生成最终可发邮件草稿

## 3. 推荐先创建的类和接口

### 3.1 启动与配置

- `CustomerChatApplication`
- `MailProperties`
- `WorkflowProperties`

### 3.2 领域枚举与对象

- `WorkflowStage`
- `WorkflowStatus`
- `InboundMail`
- `WorkflowRun`
- `WorkflowEvent`

### 3.3 邮件接入层

- `MailSourceAdapter`
- `ImapMailSourceAdapter`
- `MailFetchResult`
- `MailCleaner`

### 3.4 主链路应用层

- `MailIngestionService`
- `WorkflowRunService`
- `WorkflowStageExecutor`

### 3.5 持久化抽象

- `WorkflowRunRepository`
- `WorkflowEventRepository`

### 3.6 观测基础

- `WorkflowEventRecorder`

## 4. 切片 1 推荐的最小状态流

第一刀只需要让下面这条空链路跑通：

1. `MAIL_RECEIVED`
2. `MAIL_CLEANED`
3. `COMPLETED`

其中：

- `MAIL_RECEIVED`：代表邮件已经进入系统
- `MAIL_CLEANED`：代表正文已经做了最小清洗
- `COMPLETED`：代表当前切片的空链路成功结束

如果出错：

- 进入 `BLOCKED`

这条空链路的价值是：

- 先验证状态推进、事件记录、持久化骨架
- 避免一上来就把所有能力揉在一起

## 5. 推荐的开发顺序

### 第一步

先建项目启动骨架和配置对象：

- 应用启动类
- 配置类
- 基础目录结构

### 第二步

建主流程状态和领域对象：

- `WorkflowStage`
- `WorkflowRun`
- `WorkflowEvent`

### 第三步

建仓储接口和最小内存实现或占位实现：

- `WorkflowRunRepository`
- `WorkflowEventRepository`

如果第一刀不想先引数据库，可以先用内存实现占位，但接口命名要稳定。

### 第四步

建邮件接入抽象和 `IMAP` adapter 占位：

- `MailSourceAdapter`
- `ImapMailSourceAdapter`

这一阶段即使不接真实邮箱，也可以先做：

- 方法签名
- 返回对象
- 错误分类

### 第五步

建 `MailIngestionService` 和 `WorkflowRunService`，打通空链路。

### 第六步

补结构化事件记录和最小回放能力。

## 6. 切片 1 建议保留的接口方法风格

建议主链路接口尽量以结构化对象交互，而不是长参数列表。

例如：

- `MailSourceAdapter.fetchNewMails()`
- `MailCleaner.clean(InboundMail mail)`
- `WorkflowRunService.start(InboundMail mail)`
- `WorkflowStageExecutor.execute(WorkflowRun run)`
- `WorkflowEventRecorder.record(WorkflowEvent event)`

## 7. 切片 1 的验收标准

切片 1 完成后，至少应能验证：

- 应用可启动
- 主链路阶段枚举存在且可推进
- 一封测试邮件对象能进入 `WorkflowRun`
- 至少有一条 `workflow_run` 记录和多条 `workflow_event` 记录
- 失败场景会进入 `BLOCKED`

## 8. 切片 1 完成后再进入什么

切片 1 完成后，下一步最自然的是进入：

- 切片 2：意图归一化与路由

因为此时：

- 邮件已经能进入主流程
- 状态骨架已经存在
- 后续只需要把空节点逐步替换成真实能力

## 9. 当前建议结论

切片 1 的本质不是“做出一个能回答问题的客服”。

它的本质是：

- 先把主链路骨架搭对
- 先把状态和事件落稳
- 先给后续所有能力预留稳定插槽

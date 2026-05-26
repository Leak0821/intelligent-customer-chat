# 联调记录样例

这份文档不是模板，而是一份按当前第一版实际能力填写出来的参考样例。

## 1. 基本信息

- 日期：2026-05-26
- 分支：`feat/lock-runtime-stack`
- 提交：`c34c27a`
- 执行人：Codex
- 目标：验证第一版本地基础设施、运行时配置、售前/售后 smoke、发送闭环和演示材料链条是否可跑通

## 2. 本次使用的环境

- 应用启动方式：本地 `Spring Boot`
- Profile：`local`
- 是否启用 Nacos：是
- 是否启用 XXL-JOB：可起 admin，但本轮未强依赖调度执行
- 是否启用 IMAP：否
- 是否启用 SMTP：否，保持 `noop`
- 是否启用 Elasticsearch：是
- 是否启用真实模型：否

## 3. 本次启动的基础设施

- MySQL：已启动
- Redis：已启动
- Elasticsearch：已启动
- Nacos：已启动
- XXL-JOB Admin：已启动

## 4. 本次执行的步骤

1. `docker compose up -d mysql redis elasticsearch nacos xxl-job-admin`
2. `./scripts/publish-nacos-runtime-config.sh`
3. `mvn -Dspring-boot.run.profiles=local spring-boot:run`
4. `./scripts/local-smoke.sh after-sales`
5. `./scripts/send-lifecycle-smoke.sh`
6. `./scripts/demo-batch.sh analysis`
7. `./scripts/demo-batch.sh replay`

## 5. 本次重点验证的场景

- 售前推荐：已验证
- 售后物流：已验证
- 售后政策：已验证
- 缺关键编号触发追问：已验证
- 审核后派发：已验证

## 6. 本次结果

### 成功项

- `preflight` 能返回当前配置状态
- `runtime-config` 能从本地默认配置或 Nacos 返回运行时配置
- demo 样例能生成 `WorkflowRun`
- `analysis` 能返回 scene / subIntent / facts / knowledge / draft
- `replay` 能回看事件链和草稿
- `approve-send -> dispatch -> dispatches -> reviews` 能串起发送闭环
- 批量 demo 样例脚本可用于面试演示

### 失败项

- 本轮没有接真实 IMAP
- 本轮没有接真实 SMTP
- 本轮没有接真实模型网关

### 观察到的风险

- 如果直接对外讲成“生产可上线系统”，会过度承诺
- 如果马上接真实邮箱，最大风险还是账号权限、网络和误发副作用
- 如果后续接真实知识库，仍需要补正式灌库流程和数据治理

## 7. 本次关键输出

- `preflight` 结果：应为 `ready=true`，并能看到 database / redis / knowledge / nacos / xxl-job 等检查项
- `analysis` 结果：应能看到场景、子意图、业务事实摘要、知识摘要和草稿状态
- `replay` 结果：应能看到事件、草稿、审核记录、派发记录
- `evaluation` 结果：应能看到风险标记、发送前状态和最近审核结论
- `dispatches` 结果：应能看到至少一条派发记录
- `reviews` 结果：应能看到批准发送的审核记录

## 8. 本次遇到的问题

### 现象

- `XXL-JOB` 需要本地 admin 后台
- `Nacos` 如果没有预置配置，运行时切换意义不大
- `MySQL` 初始化 SQL 增量补充时，已有 volume 不会自动重跑

### 根因

- 第一版之前更多是工程骨架，基础设施和演示材料需要持续补平

### 修复

- 补齐 `xxl-job-admin` 的 compose 配置
- 补齐 Nacos 样例配置和发布脚本
- 补齐 smoke、发送闭环和批量演示脚本

## 9. 下次联调前要做什么

- 准备一组可用的 IMAP 测试账号
- 准备一组 SMTP 沙箱账号或邮件服务测试凭证
- 决定先接真实模型还是先接真实订单 / 物流查询

## 10. 备注

- 当前版本已经适合用于“第一版架构能力 + 工程边界意识 + 闭环意识”的面试展示
- 当前版本还不适合直接宣称“真实生产邮件客服已经 fully connected”

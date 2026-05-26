# 第一阶段运行时技术栈基线

## 1. 目的

这份文档用于把第一阶段已经明确的技术栈选择正式锁定下来，作为后续搭项目骨架、写 `pom.xml`、起 Docker 环境和开始编码的基线。

它回答的是：

- 第一阶段明确采用哪些运行时组件
- 这些组件为什么这样搭
- 哪些地方已经锁定，哪些地方仍然保留到实现时再细化

## 2. 当前已锁定的运行时基线

第一阶段当前正式采用以下基线：

- `JDK 17`
- `Maven`
- `Spring Boot 3.4.x`
- `Spring AI 1.1.7`
- `MyBatis + MyBatis-Plus`
- `MySQL 8.0.x`
- `Redis 7.x`
- `Elasticsearch 8.18.x`
- `XXL-JOB 3.x`
- `Nacos` 作为配置中心
- `Docker Compose` 承载本地基础设施

## 3. 为什么这样定

### 3.1 JDK 与 Spring 主栈

- `JDK 17`：满足 Spring Boot 3 与当前 Java 生态稳定基线
- `Spring Boot 3.4.x`：当前仍在活跃维护范围内，同时 `Spring AI 1.1.7` 官方文档明确支持 `Spring Boot 3.4.x` 和 `3.5.x`
- 第一阶段优先选 `3.4.x`，原因是比 `3.5.x` 更保守一点，但仍然处在活跃维护线

### 3.2 AI 主抽象

- `Spring AI 1.1.7`：作为第一阶段统一 AI 抽象层
- 后续对接 `DeepSeek`、Embedding、RAG、结构化输出都走这一层
- 阿里生态能力后续按需通过 `Spring AI Alibaba` 补 provider 适配，不让它接管主流程

### 3.3 持久化风格

- 采用 `MyBatis + MyBatis-Plus`
- 标准 CRUD、分页、通用表操作优先交给 `MyBatis-Plus`
- 复杂 SQL、聚合查询、特定报表或检索型 SQL 采用 `MyBatis XML`

这样做的原因：

- 更符合你的 Java 技术背景
- 对复杂查询可控，不会被 ORM 抽象反向绑架
- 后面做面试展示时也更容易解释“为什么这里要 XML”

## 4. 基础设施基线

### 4.1 MySQL

- 第一阶段业务主库采用 `MySQL 8.0.x`
- 主要承载：
  - 工作流运行记录
  - 回复草稿状态
  - 用户长期档案
  - 规则配置与审核记录

### 4.2 Redis

- 第一阶段采用 `Redis 7.x`
- 主要承载：
  - 短时上下文缓存
  - 最近几轮邮件线程上下文
  - 会话级压缩摘要
  - 去重或幂等辅助键

### 4.3 Elasticsearch

- 第一阶段知识检索采用 `Elasticsearch 8.18.x`
- 主要原因：
  - `8.18` 文档中已具备 retriever、RRF、kNN 等能力
  - 适合做 `BM25 + 向量检索 + RRF` 的混合检索
  - 版本不激进，适合第一阶段稳定落地

### 4.4 Nacos

- 第一阶段采用 `Nacos` 做配置中心
- 优先承载：
  - 非敏感业务配置
  - Prompt 模板
  - 意图枚举配置
  - 检索参数
  - 审核阈值与策略参数

### 4.5 XXL-JOB

- 第一阶段定时任务调度采用 `XXL-JOB 3.x`
- 主要承载：
  - IMAP 拉邮任务
  - 知识索引或增量同步任务
  - 周期性清理与压缩任务
  - 重试和补偿型任务

## 5. 本地开发环境原则

第一阶段明确采用：

- `Docker Compose` 承载本地基础设施

目标：

- 不污染宿主机本地数据库和搜索环境
- 保证环境可复现
- 方便后续面试演示或切换机器

第一阶段建议容器化的组件：

- `mysql`
- `redis`
- `elasticsearch`
- `nacos`
- `xxl-job-admin`

应用本身仍然本地启动，方便调试断点和开发迭代。

## 6. 配置与密钥管理建议

第一阶段建议分层处理：

### 放进 Nacos 的内容

- Prompt 模板
- 意图规则配置
- 检索参数，例如 `topK`、`rrf` 参数、召回开关
- 业务阈值和审核阈值
- 非敏感环境配置

### 不建议明文放进 Nacos 的内容

- 模型 API Key
- 邮箱账号密码
- 数据库 root 级密码
- 生产级 access token

第一阶段更稳的做法：

- 敏感信息优先放环境变量或 Docker secrets
- Nacos 先做配置中心，不先做“明文秘钥仓库”

如果后续进入更正式环境，再评估：

- Nacos 加密配置
- 外部 KMS / Vault

## 7. 上下文管理实现方向

第一阶段当前建议：

- 短时记忆：`Redis`
- 长期记忆：`MySQL`

建议分层：

- 最近 `1-5` 轮邮件上下文保留原始摘要或近原文
- 超过 `5` 轮后，引入 LLM 摘要压缩
- 例如 `6-10` 轮逐步替换为摘要块，避免上下文无限膨胀

第一阶段目标不是做复杂长期记忆系统，而是先把：

- 最近上下文可读
- 历史上下文可压缩
- 状态可回放

## 8. RAG 检索实现方向

第一阶段当前建议：

- 检索引擎：`Elasticsearch 8.18.x`
- 检索方式：混合检索
  - `BM25`
  - `kNN` 向量检索
- 融合方式：`RRF`
- 首批召回规模：每路可先取 `top10` 作为默认起点

文档组织建议：

- 采用父子文档机制表达“父文档 -> 子块”
- 父文档表示原始知识单元
- 子块表示语义切分后的检索块

但必须注意：

- Elasticsearch 官方文档明确说明 `join` 字段会增加查询代价
- 第一阶段只建议使用单层父子关系
- 不做多层 parent-child
- 必须保持稳定 routing，避免跨 shard 失联

## 9. 当前仍保留到实现时细化的点

以下内容当前只锁定方向，不先锁死具体实现细节：

- `Spring Boot 3.4.x` 具体 patch 版本
- `XXL-JOB 3.x` 具体 patch 版本
- `Redis 7.x` 与 `MySQL 8.0.x` 的具体镜像 tag
- Elasticsearch embedding 维度和索引 mapping 细节
- Nacos 的 namespace / group 规划
- Redis key 设计与过期策略

## 10. 第一阶段建议的 Java 侧依赖方向

虽然当前还没开始正式写 `pom.xml`，但方向上已经可以先锁：

- 邮件接入：`Jakarta Mail`
- Web 与管理入口：`Spring Boot Starter Web`
- Redis 接入：`Spring Data Redis`
- MySQL 持久化：`MyBatis Spring Boot Starter + MyBatis-Plus Boot3 Starter`
- Elasticsearch：官方 `Elasticsearch Java API Client`
- AI 主抽象：`Spring AI`
- 配置中心：`Nacos Config` 相关 Spring 生态接入
- 调度：`XXL-JOB` 执行器接入

第一阶段建议尽量避免：

- 同时引入多套功能重复的客户端
- 为了“以后可能会用”而先把大量 starter 全部堆进去

## 11. 当前建议结论

第一阶段现在已经不再是“技术栈待定”。

当前已明确是：

- `JDK 17 + Maven + Spring Boot 3.4.x + Spring AI`
- `MyBatis/MyBatis-Plus + MySQL`
- `Redis` 做短时上下文
- `Elasticsearch 8.18.x` 做混合检索 RAG
- `Nacos` 做配置中心
- `XXL-JOB` 做调度
- `Docker Compose` 承载本地基础设施

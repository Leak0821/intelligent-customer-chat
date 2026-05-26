# 第一阶段 `pom.xml` 依赖基线建议

## 1. 目的

这份文档用于在正式开始写 `pom.xml` 前，先把第一阶段建议依赖清单锁定到一个合理范围。

目标：

- 不漏掉主链路必需依赖
- 不一开始就堆太多无关依赖
- 给后续创建工程骨架提供清晰清单

## 2. 核心依赖方向

第一阶段建议至少包含以下依赖类别：

### 2.1 Spring Boot 基础

- `spring-boot-starter`
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`

作用：

- 启动应用
- 提供后续管理接口和审核接口入口
- 提供健康检查和基础观测

### 2.2 数据访问

- `mysql-connector-j`
- `mybatis-spring-boot-starter`
- `mybatis-plus-spring-boot3-starter`

作用：

- 连接 `MySQL`
- 承载通用 CRUD
- 支持复杂 SQL 通过 XML 扩展

### 2.3 Redis

- `spring-boot-starter-data-redis`

作用：

- 会话级上下文
- 短时记忆
- 幂等键和临时状态

### 2.4 AI 与模型层

- `spring-ai` 相关 starter

方向建议：

- 对 `DeepSeek` 优先选官方支持的接入方式
- 如果最终以 OpenAI 兼容协议接入，则用相应的 `Spring AI` OpenAI 兼容接入
- 后续接阿里 embedding，再按需补 `Spring AI Alibaba` 相关依赖

### 2.5 邮件接入

- `jakarta.mail`

作用：

- IMAP 拉取邮件
- 后续如需写入草稿箱，也继续复用这一层

### 2.6 Elasticsearch

- 官方 `Elasticsearch Java API Client`

作用：

- 混合检索
- 向量索引写入
- 检索查询与 RRF 组合

### 2.7 配置中心

- `Nacos Config` 相关 Spring 接入

作用：

- Prompt 配置
- 意图与检索参数动态配置
- 审核阈值和策略参数配置

### 2.8 调度

- `XXL-JOB` 执行器依赖

作用：

- 拉邮任务
- 重试补偿
- 索引更新
- 上下文压缩任务

## 3. 测试依赖方向

第一阶段建议至少包含：

- `spring-boot-starter-test`
- `mybatis` 相关测试支持
- 必要时补 `testcontainers`

说明：

- 如果切片 1 先不直接集成容器测试，也可以先保留普通单元测试
- 到切片 3 或切片 4 时，再评估是否补 `MySQL / Redis / Elasticsearch` 的 `Testcontainers`

## 4. 第一阶段不建议一开始就加的依赖

第一阶段不建议先加：

- 复杂工作流引擎依赖
- 多套 JSON 序列化替代库
- 多套 HTTP 客户端并存
- 过早引入消息队列客户端
- 多套搜索客户端并存

原则：

- 一类能力尽量只保留一套主依赖
- 避免还没写代码，依赖树先失控

## 5. 第一阶段建议结论

正式写 `pom.xml` 时，第一阶段应优先满足：

- Spring Boot 主干可启动
- MyBatis/MyBatis-Plus 可用
- Redis 可接
- Spring AI 可接
- Jakarta Mail 可接
- Elasticsearch Java Client 可接
- Nacos 和 XXL-JOB 有接入位置

这就足够支持第一阶段主链路骨架搭建了。

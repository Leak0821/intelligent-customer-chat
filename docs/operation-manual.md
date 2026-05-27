# 操作手册

这份手册解决两个问题：

1. 当前怎么导入 `RAG` 知识、怎么建检索索引
2. 当前版本应该怎么启动、怎么模拟、怎么跑完整闭环

这份手册按“先跑通，再细调”的思路写，不讨论后续优化方案。

## 1. 先说结论

当前版本已经有这些能力：

- 可以启动本地基础设施
- 可以启动应用
- 可以打开最小演示页
- 可以提交一封测试邮件并生成 `case`
- 可以执行审核通过、驳回、改稿、发送、重试
- 可以初始化 `Elasticsearch` 知识索引
- 可以灌入内置知识样本
- 可以手工灌入单篇知识文档
- 可以直接验证知识检索结果

当前版本还没有这些能力：

- 还没有“上传 `pdf/docx/md/csv` 后自动解析并批量灌库”的界面
- 还没有“知识库文件夹一键扫描导入”的正式批处理入口
- 还没有“知识文档管理后台”

所以你现在看到的 `RAG` 导入方式，是“接口式导入”，不是“文件上传式导入”。

## 2. 当前 RAG 知识库从哪里导入

当前有两种方式。

### 2.1 导入仓库内置知识样本

内置知识样本定义在：

- [KnowledgeSeedCatalog.java](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/src/main/java/com/leak/intelligentcustomerchat/infrastructure/knowledge/KnowledgeSeedCatalog.java)

当前内置的是首版售前 / 售后种子知识，不是最终生产知识库。

你可以通过接口查看：

```bash
curl http://127.0.0.1:8080/api/knowledge/seeds
```

这个接口只看样本，不会自动建索引，也不会自动灌入 `ES`。

### 2.2 手工导入单篇知识文档

当前已经有一个手工导入接口：

```bash
POST /api/knowledge/index/sample
```

示例：

```bash
curl -X POST http://127.0.0.1:8080/api/knowledge/index/sample \
  -H 'Content-Type: application/json' \
  -d '{
    "documentId":"custom-shipping-001",
    "title":"物流时效说明",
    "content":"当客户咨询跨境运输时效时，先区分订单处理时间和物流运输时间。如果系统没有最新节点，不要承诺准确到达日期。",
    "metadata":{
      "scene":"AFTER_SALES",
      "subIntents":"logistics_tracking,order_status",
      "source":"manual-import"
    }
  }'
```

这就是当前自定义知识导入入口。

## 3. 当前怎么建索引

当前 `Elasticsearch` 索引不是自动建的，建议手工走一次。

### 3.1 先看索引状态

```bash
curl http://127.0.0.1:8080/api/knowledge/index/status
```

如果正常，你会看到：

- 当前索引名
- 是否启用 `Elasticsearch`
- 索引是否存在

### 3.2 再执行建索引

```bash
curl -X POST http://127.0.0.1:8080/api/knowledge/index/ensure
```

这个接口负责：

- 检查索引是否存在
- 如果不存在，则创建知识索引

### 3.3 批量灌入内置种子知识

```bash
curl -X POST http://127.0.0.1:8080/api/knowledge/index/seeds
```

这个接口会把 [KnowledgeSeedCatalog.java](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/src/main/java/com/leak/intelligentcustomerchat/infrastructure/knowledge/KnowledgeSeedCatalog.java) 里的内置知识切分后写入索引。

### 3.4 验证检索是否生效

```bash
curl "http://127.0.0.1:8080/api/knowledge/search?q=tracking%20status&scene=AFTER_SALES&subIntent=logistics_tracking&topK=5"
```

如果这里能返回片段，说明当前 `RAG` 检索链至少已经通了。

## 4. 当前 RAG 检索到底用的是什么 embedding

当前本地 `local` 配置默认是：

- `app.knowledge.elasticsearch.enabled=true`
- `app.knowledge.embedding.enabled=false`

也就是说：

- 本地默认启用 `Elasticsearch`
- 但默认不启用真实模型 embedding

这时系统会回退到：

- [HashingEmbeddingService.java](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/src/main/java/com/leak/intelligentcustomerchat/infrastructure/knowledge/HashingEmbeddingService.java)

这个实现的作用是：

- 不依赖外部模型
- 先把向量索引链路跑通
- 适合本地演示和最小闭环验证

如果后面你要切成真实 embedding 模型，再打开：

```bash
APP_KNOWLEDGE_EMBEDDING_ENABLED=true
OPENAI_API_KEY=...
SPRING_AI_OPENAI_EMBEDDING_MODEL=text-embedding-3-small
```

或者切到你后面要接的兼容模型网关。

## 5. 当前最推荐的启动顺序

### 5.1 启动基础设施

```bash
docker compose up -d mysql redis elasticsearch nacos xxl-job-admin
```

### 5.2 发布本地运行时配置

```bash
./scripts/publish-nacos-runtime-config.sh
```

注意：

- 当前 `local` profile 默认 `Nacos` 是关闭的
- 这一步不是强制
- 但你后面如果要看运行时配置切换，建议先发上去

### 5.3 启动应用

```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

### 5.4 打开页面

```text
http://127.0.0.1:8080/
```

这是当前最小前端控制台入口。

## 6. 先做一次启动后检查

### 6.1 看运行前体检

```bash
curl http://127.0.0.1:8080/api/runtime-config/preflight
```

### 6.2 看业务样本

```bash
curl http://127.0.0.1:8080/api/business/orders
curl http://127.0.0.1:8080/api/business/logistics
curl http://127.0.0.1:8080/api/business/policies
```

### 6.3 看知识样本和索引状态

```bash
curl http://127.0.0.1:8080/api/knowledge/seeds
curl http://127.0.0.1:8080/api/knowledge/index/status
```

## 7. RAG 最小初始化步骤

建议第一次启动时，手工跑下面 4 步：

```bash
curl -X POST http://127.0.0.1:8080/api/knowledge/index/ensure
curl http://127.0.0.1:8080/api/knowledge/seeds
curl -X POST http://127.0.0.1:8080/api/knowledge/index/seeds
curl "http://127.0.0.1:8080/api/knowledge/search?q=product%20comparison&scene=PRE_SALES&subIntent=product_comparison&topK=5"
```

如果这 4 步都正常，就说明：

- 索引创建正常
- 内置知识可读
- 内置知识已写入索引
- 检索接口已可用

## 8. 当前怎么模拟完整闭环

我给你三种方式。

### 8.1 最推荐：直接用页面模拟

打开：

```text
http://127.0.0.1:8080/
```

页面里有两条入口：

1. 手动录入邮件
2. 运行内置场景

推荐先走“运行内置场景”，因为内置样本更稳定。

#### 页面手动操作顺序

1. 先点一个内置场景
2. 生成 `case`
3. 看 `summaryMessage`
4. 看 `scene / subIntent`
5. 看草稿内容
6. 看风险结论
7. 根据 `availableActions` 决定下一步

如果有这些按钮，说明什么意思：

- `审核通过`：当前草稿待审核
- `驳回`：当前草稿不允许直接发
- `保存改稿`：人工修改草稿
- `执行发送`：当前已放行，可模拟发送
- `重试发送`：当前发送失败待重试

### 8.2 用内置样例接口模拟

#### 先看样例目录

```bash
curl http://127.0.0.1:8080/api/workflows/demo/scenarios
```

#### 直接运行某个样例并拿到 case

```bash
curl -X POST http://127.0.0.1:8080/api/workflows/demo/scenarios/after-sales-order-status/case
```

或者：

```bash
curl -X POST http://127.0.0.1:8080/api/workflows/demo/scenarios/after-sales-logistics/case
curl -X POST http://127.0.0.1:8080/api/workflows/demo/scenarios/after-sales-missing-id/case
curl -X POST http://127.0.0.1:8080/api/workflows/demo/scenarios/after-sales-manual-review/case
curl -X POST http://127.0.0.1:8080/api/workflows/demo/scenarios/system-blocked-demo/case
```

这些分别适合模拟：

- 正常售后查询
- 缺编号追问
- 进入人工审核
- 系统阻断

### 8.3 用手工邮件接口模拟

#### 第一步：提交一封邮件并直接生成 case

```bash
curl -X POST http://127.0.0.1:8080/api/workflows/demo/case \
  -H 'Content-Type: application/json' \
  -d '{
    "from":"customer@example.com",
    "subject":"Where is my order?",
    "body":"Hi, my order ABCD1234 tracking number is ZXCV9876. Please help check the latest status."
  }'
```

返回里重点看：

- `runId`
- `summaryMessage`
- `scene`
- `subIntent`
- `draftStatus`
- `sendReadiness`
- `riskDecision`
- `availableActions`

#### 第二步：刷新 case

```bash
curl http://127.0.0.1:8080/api/workflows/<runId>/case
```

#### 第三步：如果待审核，就执行审核通过

```bash
curl -X POST http://127.0.0.1:8080/api/workflows/<runId>/approve-send \
  -H 'Content-Type: application/json' \
  -d '{
    "reviewer":"demo-admin",
    "approvalNote":"approved from manual operation"
  }'
```

#### 第四步：执行发送

```bash
curl -X POST http://127.0.0.1:8080/api/workflows/<runId>/dispatch
```

#### 第五步：查看发送记录

```bash
curl http://127.0.0.1:8080/api/workflows/<runId>/dispatches
```

#### 第六步：查看审核记录

```bash
curl http://127.0.0.1:8080/api/workflows/<runId>/reviews
```

这样就跑完一条最小闭环了。

## 9. 当前推荐你怎么操作

如果你现在是第一次上手，建议严格按这个顺序：

1. `docker compose up -d mysql redis elasticsearch nacos xxl-job-admin`
2. `SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`
3. 打开 `http://127.0.0.1:8080/`
4. 跑 `api/knowledge/index/ensure`
5. 跑 `api/knowledge/index/seeds`
6. 在页面先点一个内置场景
7. 看 `case`
8. 再手工做审核 / 改稿 / 发送
9. 最后看 `dispatches / reviews / replay / evaluation`

不要一开始就做这些事：

- 直接接真实邮箱
- 直接接真实模型
- 直接接真实订单系统
- 一上来就自己造复杂测试数据

先把“内置场景 + case + 审核 + 发送”的最小闭环跑顺。

## 10. 如果你想只用脚本

当前仓库已经有这些脚本：

- [local-smoke.sh](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/scripts/local-smoke.sh)
- [demo-batch.sh](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/scripts/demo-batch.sh)
- [send-lifecycle-smoke.sh](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/scripts/send-lifecycle-smoke.sh)
- [review-feedback-loop-smoke.sh](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/scripts/review-feedback-loop-smoke.sh)
- [demo-scenario-validate.sh](/Users/leak/Documents/code/personal/learning/ai/intelligent-customer-chat/scripts/demo-scenario-validate.sh)

最常用的是：

```bash
./scripts/local-smoke.sh after-sales
./scripts/demo-batch.sh analysis
./scripts/demo-batch.sh replay
./scripts/send-lifecycle-smoke.sh
./scripts/review-feedback-loop-smoke.sh
```

## 11. 你现在最需要知道的边界

### 已经做好的

- `RAG` 索引创建
- 内置知识导入
- 单篇知识导入
- 知识检索验证
- 工作流 case 收敛视图
- 页面闭环演示

### 还没做好的

- 自定义知识文件批量导入
- 文档上传解析
- 知识后台管理页
- 正式生产知识灌库流程

所以如果你现在问“我的真实知识库文件从哪里上传”，答案是：

- 当前版本还没有上传入口
- 现在只能通过 `index/sample` 或代码内置 `seeds` 方式导入

这不是 bug，是当前版本边界。

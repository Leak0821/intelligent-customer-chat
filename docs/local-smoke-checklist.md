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

3. 如果需要完整回放，继续查看

```bash
curl http://127.0.0.1:8080/api/workflows/<runId>/replay
curl http://127.0.0.1:8080/api/workflows/<runId>/evaluation
curl http://127.0.0.1:8080/api/workflows/<runId>/events
curl http://127.0.0.1:8080/api/workflows/<runId>/draft
```

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
- `analysis` 能返回 scene / subIntent / facts / knowledge / draft
- `replay` 能看到事件、草稿、审核或派发信息
- `knowledge` 和 `business` 接口能返回可读样本

## 5. 如果现场想快速排错

- 先看 `preflight`
- 再看 `runtime-config`
- 再看 `business` 和 `knowledge`
- 最后看 `workflow/demo`

# 联调记录模板

这份模板用于记录每一次本地联调、演示前检查或阶段性验证。

目的不是写流水账，而是把“当时怎么配、跑了什么、结果怎样、卡在哪”沉淀下来。

## 1. 基本信息

- 日期：
- 分支：
- 提交：
- 执行人：
- 目标：

## 2. 本次使用的环境

- 应用启动方式：
- Profile：
- 是否启用 Nacos：
- 是否启用 XXL-JOB：
- 是否启用 IMAP：
- 是否启用 SMTP：
- 是否启用 Elasticsearch：
- 是否启用真实模型：

## 3. 本次启动的基础设施

- MySQL：
- Redis：
- Elasticsearch：
- Nacos：
- XXL-JOB Admin：

## 4. 本次执行的步骤

建议按实际顺序填写，例如：

1. `docker compose up -d ...`
2. `./scripts/publish-nacos-runtime-config.sh`
3. `mvn -Dspring-boot.run.profiles=local spring-boot:run`
4. `./scripts/local-smoke.sh after-sales`
5. `./scripts/send-lifecycle-smoke.sh`

## 5. 本次重点验证的场景

- 售前推荐：
- 售后物流：
- 售后政策：
- 缺关键编号触发追问：
- 审核后派发：

## 6. 本次结果

### 成功项

- 

### 失败项

- 

### 观察到的风险

- 

## 7. 本次关键输出

- `preflight` 结果：
- `analysis` 结果：
- `replay` 结果：
- `evaluation` 结果：
- `dispatches` 结果：
- `reviews` 结果：

## 8. 本次遇到的问题

### 现象

- 

### 根因

- 

### 修复

- 

## 9. 下次联调前要做什么

- 

## 10. 备注

- 

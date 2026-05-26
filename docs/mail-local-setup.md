# 本地邮件接入说明

这份说明只覆盖第一版本地联调，不代表最终生产配置。

## 1. 先决定是否启用真实邮箱

### 方案 A: 只跑 demo 闭环

- 不启用真实 IMAP
- 不启用真实 SMTP
- 直接通过 `POST /api/workflows/demo` 跑邮件流程

适合：

- 先验证主链路
- 先演示意图、知识、审核、派发和回放

可直接使用：

```bash
curl http://127.0.0.1:8080/api/mail/overview
curl -X POST "http://127.0.0.1:8080/api/workflows/demo/scenarios/pre-sales-recommendation?mode=replay"
```

### 方案 B: 启用 IMAP 收信

需要配置：

- `APP_MAIL_ENABLED=true`
- `APP_MAIL_SOURCE=imap`
- `APP_MAIL_HOST`
- `APP_MAIL_PORT`
- `APP_MAIL_USERNAME`
- `APP_MAIL_PASSWORD`
- `APP_MAIL_FOLDER`

可选：

- `APP_MAIL_SSL_ENABLED=true`
- `APP_MAIL_MARK_SEEN_AFTER_FETCH=false`
- `APP_MAIL_POLLING_ENABLED=true`

适合：

- 真的想从收件箱拉邮件再推进后续流程

### 方案 C: 启用 SMTP 发信

需要配置：

- `APP_MAIL_OUTBOUND_ENABLED=true`
- `APP_MAIL_OUTBOUND_PROVIDER=smtp`
- `APP_MAIL_OUTBOUND_HOST`
- `APP_MAIL_OUTBOUND_PORT`
- `APP_MAIL_OUTBOUND_FROM_ADDRESS`
- `APP_MAIL_OUTBOUND_FROM_NAME`
- `APP_MAIL_OUTBOUND_USERNAME`
- `APP_MAIL_OUTBOUND_PASSWORD`

适合：

- 需要把最终回复真的发出去

## 2. 建议的第一版组合

第一版更建议：

- 先用 demo 跑闭环
- 先用 `noop` 发件器
- 先把 IMAP 和 SMTP 当作后续联调开关

原因：

- 能先把审核、派发、回放跑起来
- 不会一开始就被邮箱权限和收发副作用拖慢

## 3. 如果后面要真接 IMAP

建议先准备一组本地环境变量，不要直接写进主仓库配置。

```bash
APP_MAIL_ENABLED=true
APP_MAIL_SOURCE=imap
APP_MAIL_HOST=imap.example.com
APP_MAIL_PORT=993
APP_MAIL_USERNAME=...
APP_MAIL_PASSWORD=...
APP_MAIL_FOLDER=INBOX
APP_MAIL_SSL_ENABLED=true
APP_MAIL_POLLING_ENABLED=true
```

## 4. 如果后面要真发 SMTP

```bash
APP_MAIL_OUTBOUND_ENABLED=true
APP_MAIL_OUTBOUND_PROVIDER=smtp
APP_MAIL_OUTBOUND_FROM_ADDRESS=support@example.com
APP_MAIL_OUTBOUND_FROM_NAME=intelligent-customer-chat
APP_MAIL_OUTBOUND_HOST=smtp.example.com
APP_MAIL_OUTBOUND_PORT=587
APP_MAIL_OUTBOUND_USERNAME=...
APP_MAIL_OUTBOUND_PASSWORD=...
APP_MAIL_OUTBOUND_AUTH_ENABLED=true
APP_MAIL_OUTBOUND_STARTTLS_ENABLED=true
```

## 5. 这版不建议马上做的事

- 不建议先接多邮箱账号路由
- 不建议先做复杂发件队列
- 不建议先把收件、审稿、发件拆成很多微服务
- 不建议在第一版就做高频实时推送

## 6. 当前联调控制面

为了方便第一版联调，当前管理口已经提供：

- `GET /api/mail/overview`
  - 看当前是否启用邮件接入
  - 看当前轮询模式是 `manual-trigger` / `local-scheduler` / `xxl-job`
  - 看最近收件处理状态统计
- `POST /api/mail/poll`
  - 只拉取并入队，不立即执行后续流程
- `POST /api/mail/manual-enqueue`
  - 手工提交一封邮件到收件队列，适合本地演示“先入队、后处理”
- `POST /api/mail/process-pending`
  - 处理已经入队的收件记录
- `POST /api/mail/poll-and-process`
  - 拉取并直接处理，适合本地快速验证
- `POST /api/mail/receipts/{messageId}/requeue`
  - 对失败或已处理邮件重新入队
- `GET /api/mail/receipts/{messageId}`
  - 查看某一封邮件当前的收件状态
- `POST /api/mail/receipts/{messageId}/process`
  - 只处理指定 `messageId` 的那一封邮件，适合异步联调
- `GET /api/mail/receipts`
  - 查看最近收件记录

如果要直接演示异步链路，可执行：

```bash
./scripts/async-mail-smoke.sh
```

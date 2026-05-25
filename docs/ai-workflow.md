# AI 协作工作流

## 1. 目的

这份文档定义本项目在 AI 协作开发中的标准工作流，核心是把不同工具的职责分开，避免边探索边实现、边实现边改需求。

适用范围：

- 需求不够清晰
- 需要多人或多 agent 协作
- 需要先沉淀规范再进入实现
- 需要让后续每轮开发有统一门禁

当前标准工具栈：

- `oh-my-codex`
- `superpowers brainstorming`
- OpenSpec
- `gstack`
- `superpowers implementation`

## 2. 一句话原则

```text
oh-my-codex 负责组织流程；
superpowers brainstorming 负责想清楚问题；
OpenSpec 负责把结论写成合同；
gstack 负责挑错和校验；
superpowers implementation 负责按合同执行。
```

## 3. 工具角色分工

| 工具 | 角色 | 主要职责 | 不应该做什么 |
|---|---|---|---|
| `oh-my-codex` | 流程编排层 | 组织阶段、记录状态、推进任务 | 不作为最终需求规格源 |
| `superpowers brainstorming` | 探索层 | 发散需求、比较路径、识别风险、收敛 MVP | 不直接作为实现依据 |
| OpenSpec | 规格合同层 | 固化 proposal、spec、tasks、design | 不承担开放式头脑风暴 |
| `gstack` | 审查层 | 反方审查、挑战假设、检查 spec 与 diff | 不默默接管主流程或扩大范围 |
| `superpowers implementation` | 执行层 | 按任务实现、验证、修复、必要时多 agent 并行 | 不重新定义需求边界 |

## 4. 标准总流程

```text
治理或任务启动
  ↓
oh-my-codex 组织阶段和状态
  ↓
superpowers brainstorming 探索不确定需求
  ↓
人工确认 / 收敛
  ↓
OpenSpec 创建 proposal / spec / tasks / design
  ↓
gstack 审查规格
  ↓
根据审查意见更新 OpenSpec
  ↓
superpowers implementation 按 tasks 执行
  ↓
测试 / 修复
  ↓
gstack 审查最终 diff
  ↓
OpenSpec archive
```

注意：

- 不是每次都要完整走完全部阶段
- 但一旦进入实现，OpenSpec 必须成为唯一合同源
- 如果本轮用户明确说“先不 brainstorm”，就只做治理、规范或 OpenSpec 准备，不提前写业务代码

## 5. 任务分级

### 5.1 小任务

典型场景：

- 局部 bug 修复
- 文案改动
- 小型脚本调整
- 简单测试修补

默认流程：

```text
inspect -> edit -> targeted verification -> report evidence
```

规则：

- 默认不强制 OpenSpec
- 不引入无关重构
- 如果改动开始扩散，必须升级为中任务或大任务

### 5.2 中任务

典型场景：

- 新功能
- 用户可感知行为变化
- 跨多个文件的流程调整
- 需要明确验收标准的改动

默认流程：

```text
discovery if needed -> OpenSpec -> optional gstack review -> implementation -> verification
```

规则：

- 需要 OpenSpec
- 风险不低时推荐 `gstack` 审查

### 5.3 大任务

典型场景：

- 架构调整
- 数据模型变化
- 公共 API 或兼容性变化
- 鉴权、迁移、部署或跨模块重构

默认流程：

```text
discovery -> OpenSpec proposal/spec/tasks/design -> gstack review -> implementation -> verification -> review-diff -> archive
```

规则：

- 必须先有完整 OpenSpec
- 默认先审规格，再写代码

## 6. 阶段门禁

允许的阶段：

1. `governance`
2. `discovery`
3. `openspec`
4. `review-spec`
5. `implement-task`
6. `test-fix`
7. `review-diff`
8. `archive`

每个阶段的边界：

| 阶段 | 允许做什么 | 不允许做什么 |
|---|---|---|
| `governance` | 写规范、改流程、补模板、定边界 | 不写业务功能 |
| `discovery` | 探索需求、收集问题、比较方案 | 不直接改业务代码 |
| `openspec` | 创建或更新 OpenSpec | 不实现功能 |
| `review-spec` | 审查 proposal/spec/tasks/design | 不跳过合同直接写代码 |
| `implement-task` | 只实现当前任务 | 不扩 scope，不抢跑下一个任务 |
| `test-fix` | 验证并修当前变更的问题 | 不顺手修无关问题 |
| `review-diff` | 审查当前 diff | 不默认自动修 |
| `archive` | 固化稳定规格 | 不替代正在进行中的 change 文档 |

## 7. 不确定需求怎么处理

当出现以下情况时，默认判定为不确定需求：

- 目标模糊
- 验收标准缺失
- 多条实现路径都成立
- 影响用户体验、知识边界、权限、安全、升级策略

处理规则：

1. 先进入 `discovery`
2. 用 `superpowers brainstorming` 发散和收敛
3. 只把确认结论放进 OpenSpec
4. 探索笔记保留在 `research/`

硬规则：

- brainstorming 结论不是合同
- 未进入 OpenSpec 的候选方案不能直接实现

## 8. 多 agent 执行规则

`superpowers implementation` 允许多 agent，但只能在边界清楚时使用。

适合并行的情况：

- 多个独立分析任务
- 多个互不冲突的阅读任务
- 一个主实现 + 一个独立审查
- 按任务拆开的并行实现

不适合并行的情况：

- 需求还没定
- OpenSpec 还没收敛
- 多个 agent 会同时编辑同一块不稳定逻辑
- 需要在执行中重新定义范围

收敛规则：

- OpenSpec 永远是唯一合同源
- 必须有一个主负责者统一合并结果
- 最终只对合并后的结果做验证和交付

## 9. 文档落点

| 文档位置 | 存什么 | 是否可直接指导实现 |
|---|---|---|
| `research/` | 探索记录、对比分析、问题清单 | 否 |
| `openspec/changes/` | 当前进行中的正式变更合同 | 是 |
| `openspec/specs/` | 已稳定生效的系统规格 | 是 |
| `docs/` | 团队长期规则、流程、边界 | 间接，是上位约束 |

关于 `oh-my-codex` 的过程性记录：

- 如果只是本地过程状态、临时日志、草稿编排信息，可以留在本地
- 如果是后续要复用的探索结论，应转写到 `research/`
- 如果已经成为正式实施边界，应进入 OpenSpec

## 10. 当前项目的执行建议

考虑到这个仓库还没有正式业务代码，当前优先级应为：

1. 先稳定治理规则
2. 再稳定 AI 工作流规则
3. 再定义项目范围
4. 再做第一条闭环链路的 OpenSpec
5. 最后才进入实现

这意味着现在可以先把工具协作规范写清楚，但不需要提前启动业务 brainstorming 或多 agent 编码。

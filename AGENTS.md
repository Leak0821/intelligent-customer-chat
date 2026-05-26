# 智能客服项目 Agent 合同

## 适用范围

当前仓库仍处于智能客服项目的初始化与规范建设阶段。

- 代码实现还不是当前第一优先级。
- 文档、流程和变更纪律优先。
- 当前阶段暂不锁定技术栈。

## 指令优先级

当规则冲突时，按以下顺序处理：

1. 用户明确指令
2. 本文件
3. `openspec/changes/` 下当前激活的 OpenSpec 变更
4. `docs/` 下稳定的项目规则
5. 本地临时假设

## 项目默认约束

- 默认使用中文回复，除非用户明确要求其他语言。
- 写代码前先澄清范围、成功标准和非目标。
- 优先选择能证明想法成立的最小改动。
- 不做顺手重构。
- 未经明确确认，不提前锁定框架、供应商或架构。

## 工作流工具栈

本仓库当前标准工作流为：

- Codex CLI / Codex app：执行环境
- `oh-my-codex`：流程编排层
- `superpowers brainstorming`：需求探索层
- OpenSpec：需求合同层
- `gstack`：规格与 diff 审查层
- `superpowers implementation`：实现执行层，可在合适场景下使用多 agent

角色规则：

- `oh-my-codex` 负责组织流程、记录状态、控制阶段。
- `superpowers brainstorming` 负责探索和比较方案，但不直接定义最终范围。
- OpenSpec 负责定义最终行为、验收标准和任务边界。
- `gstack` 负责挑战假设和审查产物，但不能默默扩大范围。
- `superpowers implementation` 只按 OpenSpec 执行，不重新定义需求。

## 任务分级

默认按以下方式划分任务，除非用户明确指定其他级别。

### 小任务

适用场景：

- 局部小修复
- 文案修改
- 小配置调整
- 简单脚本或测试修补

规则：

- OpenSpec 可选。
- 按 `inspect -> edit -> targeted verification -> report evidence` 执行。
- 改动要局部、可回退。

### 中任务

适用场景：

- 新行为
- 流程变化
- 多文件功能改动
- 需要明确验收标准的任务

规则：

- 实现前需要创建或更新 OpenSpec。
- 有一定风险时建议经过 `gstack` 审查。

### 大任务

适用场景：

- 架构调整
- 数据模型变化
- 公共 API 变化
- 鉴权、权限、部署、迁移或兼容性敏感改动

规则：

- 需要 `proposal.md`、`spec.md`、`tasks.md`，通常还需要 `design.md`。
- 实现前默认要经过 `gstack` 审查。

## 不确定需求处理

出现以下情况时，应视为不确定需求：

- 目标模糊
- 验收标准缺失
- 存在多条合理路径
- 产品、体验、架构、API、安全或数据决策仍未收敛

规则：

- 不直接跳进实现。
- 中大任务先用 `superpowers brainstorming` 做探索。
- 只有确认结论才能进入 OpenSpec。
- 探索笔记保存在 `research/`，不作为实现合同。

如果用户明确说“本轮先不 brainstorm”，则当前回合只做治理、规范或文档工作，不提前写业务代码。

## 阶段控制

当用户将本轮限制在某个阶段时，不得跨到后续阶段。

允许阶段：

1. `governance`
2. `discovery`
3. `openspec`
4. `review-spec`
5. `implement-task`
6. `test-fix`
7. `review-diff`
8. `archive`

阶段规则：

- `governance`：只做仓库规则、流程文档、模板和边界定义
- `discovery`：只做研究和探索，不改业务代码
- `openspec`：只创建或更新 OpenSpec
- `review-spec`：只审查规格产物
- `implement-task`：只实现当前 OpenSpec 中被选中的任务
- `test-fix`：只验证并修复与当前变更相关的问题
- `review-diff`：只审查当前 diff
- `archive`：把稳定行为沉淀到 `openspec/specs/`

补充规则：

- 一旦某个变更已经进入实现阶段，优先遵守该变更目录下的执行守则类文档，例如 `execution-playbook.md`
- 仓库级规则负责通用约束，当前激活变更的执行守则负责“这一轮到底做什么、不做什么”

## 多 Agent 规则

`superpowers implementation` 只有在能够提升效率且不造成责任不清时，才允许使用多 agent。

允许：

- 独立分析并行
- 并行代码阅读
- 并行验证或审查
- 按明确任务边界拆开的并行实现

不允许：

- 多个 agent 同时修改同一块尚未收敛的需求
- OpenSpec 未明确前就并行实现
- 由并行 agent 各自定义冲突范围

收敛规则：

- OpenSpec 始终是唯一合同源。
- 必须有一个任务负责人统一合并结果。
- 最终只对合并后的结果做验证和交付。

## 文档工作流

中大任务或不确定任务，默认按以下流程推进：

1. `oh-my-codex` 定义任务与阶段
2. `superpowers brainstorming` 在需要时做探索
3. 确认结论进入 `research/` 或直接进入 OpenSpec
4. 在 `openspec/changes/<change-id>/` 下创建变更
5. 风险较高时由 `gstack` 审查规格
6. `superpowers implementation` 按任务逐步执行
7. 收集验证证据
8. 将稳定行为沉淀到 `openspec/specs/`

规则：

- `research/` 是探索材料，不具备实现权威性
- `openspec/changes/` 一旦建立，即为实现合同
- 不得实现未写入当前 OpenSpec 的行为
- 对于局部小改且不改变明显用户行为的任务，可跳过 OpenSpec

## 工程默认原则

进入代码工作时，默认遵守：

- 先想清楚再写代码
- 方案尽量简单
- 只做外科手术式改动
- 先定义成功标准和验证证据
- 不硬编码密钥
- 不把未验证假设偷偷写进代码

## 必需交付证据

在声称任务完成前，必须报告：

- 改动文件
- 执行命令
- 验证结果
- 已知风险或跳过项

## 仓库边界

可以提交：

- 可持续维护的 Markdown 文档
- 可复用脚本
- 脱敏示例和模板
- 已确认的 OpenSpec 与 research 产物

只留本地：

- 密钥、token、私钥、cookie
- `.local-ssh/`
- `.codex-home/`
- `.codex-local/`
- 含本机绝对路径的配置
- 缓存、克隆工具源码、临时产物

## 客服领域约束

任何面向客户的行为变化，在规格里都应至少说明：

- 用户或操作角色
- 渠道或入口
- 触发条件与输入
- 知识来源或依据来源
- 回复边界与拒答规则
- 升级或转人工条件
- 日志、回放或评估证据

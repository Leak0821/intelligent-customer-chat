# OpenSpec 变更区

`openspec/changes/` 用于保存进行中的正式变更合同。

适用场景：

- 变更属于中任务或大任务
- 需求仍存在不确定性
- 用户可见行为可能变化
- 存在多条实现路径

推荐结构：

```text
openspec/changes/<change-id>/
  proposal.md
  spec.md
  tasks.md
  design.md   # 可选
  review-guide.md   # 复杂变更可选
```

规则：

- 这里只存放已确认的决策
- 探索材料留在 `research/`
- 一旦开始实现，这里的激活变更就是唯一合同源
- 对复杂变更，建议补一份审阅入口，把阅读顺序和审查重点写清楚

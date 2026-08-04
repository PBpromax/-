# SPEC_PROCESS: CampusHub AI4SE Spec Process

## 1. 过程概览

本项目从一个已有的校园互助平台代码库出发，整理为 AI4SE B 类“非 harness 应用类项目”。过程重点不是重新发明业务，而是把已有系统转化为可审计、可冷启动、可测试、可分发的个人提交项目。

使用的主要智能体是 OpenAI Codex。由于当前项目已经有代码和历史课程文档，本次流程包含一次现实约束下的偏离：没有在实现代码前从零完成 Superpowers brainstorming 和 PLAN，而是对已有项目进行逆向规约、交付物重构和验证补证。该偏离会在 `AGENT_LOG.md` 和 `REFLECTION.md` 中说明。

## 2. Brainstorming 关键节点

### 节点 1：提交类型选择

问题：CampusHub 是否适合按 Coding Agent Harness 提交？

结论：不适合。项目没有 autonomous agent 主循环、工具分发、LLM 自我修正机制，也不调用外部 LLM API。按 AI4SE B 类应用项目提交更准确。

影响：SPEC 中明确排除 harness 特定要求，不补 mock LLM 测试或 agent 内核。

### 节点 2：功能深度判断

问题：是否需要继续增加业务功能？

结论：暂不新增。现有注册登录、需求、订单、评价、通知、管理后台已超过 3 个清晰模块，且有后端测试。比起加功能，交付物、CI、分发和安全说明更影响本次作业评分。

影响：PLAN 以提交包整理为主，不引入数据库迁移或新接口。

### 节点 3：凭据安全边界

问题：没有 LLM API Key 时如何满足凭据要求？

结论：说明本项目不调用外部付费 LLM API；凭据范围转为 JWT secret、数据库密码、Redis 配置。使用 `.env.example` 展示配置项，真实 `.env` 不提交。

影响：SPEC 和 README 均加入凭据威胁模型与生产覆盖要求。

### 节点 4：旧交付物处理

问题：旧 P0-P4 团队课程交付物是否保留？

结论：删除。AI4SE 要求个人项目，旧团队材料会制造范围混淆和评审噪声。

影响：删除 `docs/P0` 到 `docs/P4` 和 `outputs`，保留源码与 AI4SE 交付文档。

## 3. 关键迭代记录

### 迭代 1：从“功能补强”改为“提交包补齐”

初始判断倾向于继续补推荐、举报等功能。检查作业要求后发现硬性项集中在 Superpowers 流程、SPEC/PLAN、CI、分发、反思和安全。最终决定不新增业务功能，优先修补工程闭环。

### 迭代 2：README 端口策略修正

项目文档曾出现后端 `8080`、前端代理 `8092`、MySQL `3307/3310`、Redis `6379/6381` 不一致。整理后 README 选择当前前端代理实际使用的策略：前端 `5173`，后端本地开发 `8092`，MySQL `3307`，Redis `6379`。

### 迭代 3：CI 从 smoke test 改成真实验证

`.gitlab-ci.yml` 曾退化为 Alpine echo smoke test，不满足期末要求。整理后计划改为包含 `unit-test` 的真实 GitLab CI，并增加前端 build 与 Docker build 验证。

### 迭代 4：反思报告处理方式

作业要求反思报告必须由本人撰写。用户选择生成“辅助初稿”。因此 `REFLECTION.md` 开头明确标注 AI 辅助性质，最终提交前必须由本人重写和确认。

## 4. 冷启动验证计划

正式提交前需要使用不同于主开发智能体的新 session 或不同 agent，仅提供 `SPEC.md` 和 `PLAN.md`，要求其完成 1-2 个任务或审查可执行性。

推荐冷启动任务：

1. 仅凭 SPEC/PLAN 检查 README 是否能支持全新机器启动。
2. 仅凭 SPEC/PLAN 检查 `.gitlab-ci.yml` 是否满足 `unit-test` 要求。

需要记录的问题：

- agent 是否对端口、凭据、Docker 运行方式产生疑问。
- agent 是否误以为项目需要 LLM API Key。
- agent 是否把旧团队文档当作本次提交物。
- agent 对测试命令和验收标准是否能独立执行。

## 5. 已发现的 SPEC 缺陷与修订

- 缺陷：旧 README 中团队分工和 AI4SE 个人项目要求冲突。
  - 修订：README 改为个人 AI4SE 提交视角，说明历史团队材料已移除。
- 缺陷：旧配置说明存在端口不一致。
  - 修订：README 明确本地开发统一使用 `5173` + `8092`。
- 缺陷：旧文档没有凭据威胁模型。
  - 修订：SPEC 新增安全与凭据威胁模型，明确本项目不使用 LLM API Key。
- 缺陷：旧 CI 不运行测试。
  - 修订：GitLab CI 新增 `unit-test` job。

## 6. 偏离说明

- 没有从零开始实施 Superpowers 七步流，因为项目已有代码基础。
- TDD 证据主要来自现有后端测试和本次验证记录，不是所有历史功能都能证明“先红后绿”。
- Open Design 未正式引入，原因是项目已有 Vue/Element Plus UI，期末整理优先保证工程交付闭环。
- GitHub/GitLab PR 历史若不足，需要在最终提交说明中如实承认，并用当前整理 commit、CI 结果和文档证据补足可审计性。

# AGENT_LOG: CampusHub AI4SE Submission

> 本日志按时间记录 AI 协作、人工决策、验证结果和偏离说明。时间采用 Asia/Shanghai。

## 2026-07-24 20:00 - Task 0 - 项目盘点

- 技能/工具：Codex 仓库检查、文件搜索、README/作业要求比对。
- 关键上下文：读取 AI4SE 通用要求和 B 类项目要求，检查 CampusHub README、演示说明、CI、测试目录和 Docker 配置。
- 发现：
  - 项目适合 B 类应用项目，不适合 harness。
  - 缺少 `SPEC.md`、`PLAN.md`、`SPEC_PROCESS.md`、`AGENT_LOG.md`、`REFLECTION.md`。
  - `.gitlab-ci.yml` 是 smoke test，不满足 `unit-test` job 要求。
  - 后端测试存在，前端可构建。
- 人工决策：优先补齐提交包，不新增业务功能。

## 2026-07-24 20:07 - Task 0 - 本地验证基线

- 命令：`cd backend && mvn test`
- 结果：通过，85 个测试，0 failures，0 errors。
- 命令：`cd frontend && npm run build`
- 结果：通过，Vite 生产构建成功；存在 chunk size warning，但不阻塞构建。
- 人工判断：现有功能具备提交基础，主要风险转为文档、CI 和分发说明。

## 2026-07-24 20:20 - Task 1 - 整理计划确认

- 用户要求：按计划实现，删除旧 P0-P4 交付物和 outputs。
- 人工决策：
  - 删除旧团队课程材料，避免与个人 AI4SE 提交冲突。
  - 保留 `DEVELOPMENT.md` 和 `BRANCH_STRATEGY.md`，暂不扩大删除范围。
  - `REFLECTION.md` 生成 AI 辅助初稿，最终提交前由本人改写确认。

## 2026-07-24 20:25 - Task 2 - 文档与 CI 实施

- 变更：
  - 新增 `SPEC.md`、`PLAN.md`、`SPEC_PROCESS.md`、`AGENT_LOG.md`、`REFLECTION.md`。
  - 更新 `README.md` 为 AI4SE 提交视角。
  - 新增 `.env.example`。
  - 更新 `.gitlab-ci.yml`，加入 `unit-test`、`frontend-build`、`docker-build`。
  - 删除 `docs/P0` 到 `docs/P4` 和 `outputs`。
- 人工判断：不改业务代码，降低引入功能回归的风险。

## 2026-07-24 20:30 - Task 3 - 最终验证记录

- 已执行：
  - `cd backend && mvn test`：通过，85 tests，0 failures，0 errors。
  - `cd frontend && npm ci && npm run build`：通过；npm audit 报 4 个依赖漏洞，Vite 报 chunk size warning，均未阻塞构建。
  - `find docs -maxdepth 2 -type f`：旧 P0-P4 文件已清理，仅保留 `docs/.gitkeep`。
  - `docker compose -f docker-compose-prod.yml build`：首次失败因为 Docker daemon 未启动；启动 Docker Desktop 后重试，失败原因变为 Docker Hub oauth token 请求超时，不是项目 Dockerfile 编译错误。提交前需要在网络稳定环境或 GitLab CI 中重跑并记录 pass。
- CI 调整：`docker-build` job 改为分别执行 `docker build backend` 和 `docker build frontend`，降低 CI 对 docker compose 插件的依赖。

## 偏离与教训

- 偏离：项目并非从空仓库开始，无法完整还原所有历史功能的“先红后绿”TDD 过程。
- 对策：如实记录偏离，保留现有测试结果，并在后续改动中坚持先写测试再实现。
- 教训：AI4SE 评分重点不是“页面数量”，而是规约清晰度、可验证性、凭据治理、分发和过程证据。

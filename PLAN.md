# PLAN: CampusHub AI4SE Submission Package

## 1. 目标

把现有 CampusHub 整理成 AI4SE B 类应用项目提交包。实现重点不是新增业务功能，而是补齐规约、计划、过程、反思、CI、分发和安全说明，让助教可以从仓库冷启动运行和验证项目。

## 2. Task 列表

### Task 1: 清理旧课程交付物

- 目标：移除旧《软件工程与计算 II》团队阶段文档和生成物，降低提交噪声。
- 涉及文件：`docs/P0`、`docs/P1`、`docs/P2`、`docs/P3`、`docs/P4`、`outputs`。
- 实现要点：删除旧目录，保留 `docs/.gitkeep`。
- 验证步骤：运行 `find docs -maxdepth 2 -type f`，确认不再出现 P0-P4 文件；运行 `git status --short` 查看删除清单。

### Task 2: 补充 SPEC

- 目标：生成符合 AI4SE 通用要求和 B 类项目要求的 `SPEC.md`。
- 涉及文件：`SPEC.md`。
- 实现要点：覆盖问题陈述、用户故事、模块规约、非功能需求、安全与凭据、架构、数据模型、分发、技术选型、验收标准、风险。
- 验证步骤：人工核对作业要求清单；确认文档说明本项目不调用外部付费 LLM API。

### Task 3: 补充 PLAN

- 目标：生成可执行、可审计的 `PLAN.md`。
- 涉及文件：`PLAN.md`。
- 实现要点：按 2-5 分钟粒度描述整理任务，写清目标、文件、实现要点、验证步骤、依赖与可并行项。
- 验证步骤：确认每个任务都有客观验证方式。

### Task 4: 补充过程证据

- 目标：补齐 `SPEC_PROCESS.md` 与 `AGENT_LOG.md`。
- 涉及文件：`SPEC_PROCESS.md`、`AGENT_LOG.md`。
- 实现要点：记录 brainstorming、关键迭代、冷启动验证计划、CI/测试执行、人工决策和偏离说明。
- 验证步骤：确认文档包含 Superpowers 七步工作流映射与偏离记录。

### Task 5: 补充反思报告候选稿

- 目标：生成 `REFLECTION.md`，作为本人最终确认和必要改写的材料。
- 涉及文件：`REFLECTION.md`。
- 实现要点：覆盖 Superpowers、TDD、subagent、SPEC/PLAN、prompt/context、凭据与分发、批判性思考。
- 验证步骤：确认文末明确说明 AI 辅助范围，并提醒最终内容需要本人确认真实经历与事实准确性。

### Task 6: 更新 README 与安全配置

- 目标：让 README 满足作业最终交付清单。
- 涉及文件：`README.md`、`.env.example`。
- 实现要点：写清项目简介、安装运行、测试、Docker 分发、目录结构、安全边界、已知限制、线上部署 URL 占位；新增不含真实凭据的 `.env.example`。
- 验证步骤：从 README 可独立找到目录结构、JDK/Node/Docker 版本、数据库初始化方式、管理员账号准备方式、运行命令、测试命令、分发命令、健康检查和安全说明；确认 `.env.example`、`docker-compose-prod.yml`、`application-prod.yml` 变量名一致。

### Task 7: 更新 GitLab CI

- 目标：替换 smoke test，满足 `.gitlab-ci.yml` 必须包含 `unit-test` job 的要求。
- 涉及文件：`.gitlab-ci.yml`。
- 实现要点：增加后端测试、前端构建和 Docker 构建验证。
- 验证步骤：静态检查 `unit-test` job 存在；提交后查看最后一次 CI/CD pass。

### Task 8: 本地验证

- 目标：证明提交包没有破坏可运行性。
- 涉及命令：`mvn test`、`npm ci && npm run build`、`docker compose -f docker-compose-prod.yml build`。
- 验证步骤：记录命令结果到 `AGENT_LOG.md`；若 `mvn test` 或前端构建失败，必须修复并复验；若 Docker 构建因 Docker Hub 网络、daemon 状态等外部环境失败，可记录原因并以 GitLab CI 复验结果为最终证据。

## 3. 依赖关系

- Task 1 可独立执行。
- Task 2 和 Task 3 互相依赖，应先写 SPEC，再让 PLAN 对齐 SPEC。
- Task 4 依赖 Task 2 和 Task 3 的内容。
- Task 5 依赖 Task 4 的过程证据。
- Task 6 和 Task 7 可与文档任务并行。
- Task 8 必须在所有文件整理后执行。

## 4. 可并行部分

- README/`.env.example` 与 `.gitlab-ci.yml` 可并行。
- SPEC 与 PLAN 可由两个 subagent 草拟，但最终需要人工统一术语和验收标准。
- 测试验证和文档清单检查可并行执行。

## 5. 完成定义

- AI4SE 五份核心文档存在且内容覆盖作业要求。
- README 可支持陌生用户冷启动。
- 旧 P0-P4 和 outputs 已删除。
- `.gitlab-ci.yml` 有 `unit-test` job。
- 后端测试与前端构建通过。
- Docker 生产镜像构建结果已记录。

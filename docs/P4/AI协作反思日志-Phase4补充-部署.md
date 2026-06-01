# AI 协作反思日志 — Phase 4 补充：生产环境部署

团队：海底小纵队 日期：2026年6月1日 补充人：仲嘉辉

---

## 1. 本阶段（部署相关）AI 使用清单

| 任务 | AI工具 | Prompt摘要 | AI输出质量(1-5) | 人工修改幅度(%) |
| :---- | :---- | :---- | :---- | :---- |
| Docker 生产环境部署方案设计 | Claude Code | 提供服务器配置（2 vCPU / 4 GiB）和公网 IP，要求规划部署架构、列出需要新增和修改的文件 | 4 | 15% |
| 后端 Dockerfile 编写 | Claude Code | 指定多阶段构建（Maven 编译 → JRE 运行）、轻量化要求、阿里云镜像加速 | 4 | 10% |
| 前端 Dockerfile 编写 | Claude Code | 指定多阶段构建（Node 构建 → Nginx 托管）、含 SPA 回退 | 5 | 5% |
| Nginx 反向代理配置 | Claude Code | 要求实现 API 代理、静态文件托管、SPA history 模式支持、gzip 压缩 | 5 | 5% |
| docker-compose-prod.yml 编排 | Claude Code | 指定 4 服务编排、内网隔离、环境变量注入、数据持久化 | 4 | 10% |
| deploy.sh 一键部署脚本 | Claude Code | 要求含 git pull、构建、启动、健康检查全流程 | 4 | 10% |
| 环境变量统一修正 | Claude Code | 发现 http.js 与 http.ts 使用不同变量名（VITE_API_BASE vs VITE_API_BASE_URL），要求修复 | 5 | 0% |
| 数据库编码问题排查 | Claude Code | 中文插入 MySQL 后变成乱码，要求定位原因并修复 | 3 | 30% |
| 智能推荐排序完善 | Claude Code | 要求在现有推荐基础上增加"同分类优先"排序，并编写测试 | 4 | 15% |

---

## 2. AI 助力分析（Q1）

**AI 在部署方案设计中的价值**：AI 一次性给出了完整的部署架构图、7 个新增文件和 2 个修改文件清单，涵盖了 Dockerfile、Nginx 配置、docker-compose 编排、部署脚本等所有方面。人工设计同样方案需要查阅大量文档和模板，AI 将这个过程压缩到了几分钟。

**AI 在配置文件生成中的价值**：Dockerfile 的结构（分阶段构建、缓存优化）、Nginx 配置的语法（`try_files`、`proxy_pass`、gzip）、docker-compose 的格式——这些都是标准化程度高的模板化工作，AI 的输出几乎可以直接使用，人工只需微调参数。

**AI 在代码审查中的价值**：AI 通过全局搜索发现了 `http.js`（使用 `VITE_API_BASE`）和 `http.ts`（使用 `VITE_API_BASE_URL`）使用不同环境变量名的问题。这种跨文件的一致性问题，人工很难在短期内发现，AI 的全局搜索能力体现出了明显优势。

**AI 在 Bug 修复中的价值**：MySQL 中文乱码问题是由 bash 命令行传递中文到 Docker 容器时的编码转换引起的。AI 尝试了多种方案（docker exec 直接执行 → docker exec + heredoc 文件 → 最终通过容器内写文件解决），展现了系统化的问题排查能力。

**AI 在功能完善中的价值**：智能推荐增加"同分类优先"排序时，AI 不仅给出了 SQL 改写方案，还同步补充了测试用例（新增 3 个用户、2 条种子数据、2 个测试方法），确保功能改动有测试覆盖。测试一次性通过（15/15），没有引入回归问题。

---

## 3. AI 误导分析（Q2）

### 服务器上线实战中的 Bug 案例（本次补充）

在实际部署到阿里云 ECS（IP: 101.132.23.129）的过程中，遇到了 4 个关键问题，其中 Bug #1 和 #2 充分展示了 AI 的局限性。

**Bug #5：HTTP API 前缀重复导致 401（AI 直接引入的 Bug）**

| 维度 | 详情 |
|------|------|
| 问题描述 | 服务器部署后注册页面点击注册显示"未认证或凭证失效" |
| 根因定位 | AI 建议将 `http.js` 默认值改为 `/api`，同时 `.env.production` 也设为 `VITE_API_BASE_URL=/api`。但所有 API 调用路径已是 `/api/v1/...`，拼接后变成 `/api/api/v1/...`，该路径不匹配 JWT 拦截器的排除规则，被拦截返回 401 |
| 暴露的 AI 缺陷 | AI 在修改代码时只关注"统一变量名"这一个局部问题，没有追踪该变量的完整使用链路（定义 → 拼接 → 请求 → Nginx 代理 → 后端路由 → 拦截器匹配），属于典型的"修了表面、遗漏全貌" |
| 最终修复 | `http.js` 默认值改为空字符串、`.env.production` 也设空值。API 路径自然由 Nginx `location /api/` 转发 |
| 教训 | 涉及请求链路的全局修改，AI 必须主动验证：这个值在哪里定义、在哪里拼接、最终 URL 是什么、能否到达正确的后端端点 |

**Bug #6：JWT Secret 默认值格式不合法**

| 维度 | 详情 |
|------|------|
| 问题描述 | 后端容器反复崩溃重启，日志 `Illegal base64 character: '-'` |
| 根因 | `application-prod.yml` 中 JWT secret 默认值 `change-me-in-production` 含连字符，jjwt 库无法解析 |
| AI 责任分析 | AI 在编写 `application-prod.yml` 时写了不合法默认值作为占位符，期望用户手动替换。但未能明确警告"该值会在无环境变量时直接被 jjwt 解析，必须替换为合法 Base64" |
| 最终修复 | 通过 `openssl rand -base64 32` 生成合法密钥，以环境变量注入 |

**Bug #7：Docker Hub 拉取超时**

| 维度 | 详情 |
|------|------|
| 问题描述 | `docker compose build` 构建阶段拉取基础镜像时 i/o timeout |
| 根因 | 阿里云 ECS 在国内，无法直接访问 Docker Hub |
| AI 是否预判 | 否。AI 在制定部署方案时未考虑"国内服务器访问 Docker Hub 会超时"这一网络环境因素 |
| 修复 | 配置阿里云 Docker 镜像加速器（registry.cn-hangzhou.aliyuncs.com） |

### 开发阶段 Bug 案例（之前已记录）

**Bug #1：MySQL 容器 crash → 订单查询失败**

| 维度 | 详情 |
|------|------|
| 问题现象 | 发布需求时报"服务端异常" |
| 根因 | 执行 ALTER TABLE 后 MySQL 容器异常退出，后端连接池全部失效 |
| AI 是否准确定位 | 是。AI 通过 `docker ps` 发现容器 Exited (2)，并识别出 `Communications link failure` 错误 |
| AI 修复方案 | 重启容器 + 重启后端 |
| 方案是否有效 | 部分有效。容器重启后需要重新执行 ALTER TABLE，而且用户数据可能丢失 |
| 遗留问题 | 用户注册数据在崩溃中丢失，但 JWT token 仍有效，导致"已登录但查不到用户"的诡异状态 |

**Bug #2：数据库表结构与实体类不匹配**

| 维度 | 详情 |
|------|------|
| 问题现象 | 登录报"服务端异常"，实际是 `Unknown column 'role'` |
| 根因 | `mysql-init/init.sql`（Docker 入口）与 `backend/.../db/migration/.../init.sql`（开发用）不同步，旧版缺少 `role` 等字段 |
| AI 是否准确定位 | 是。通过日志中的 SQL 错误信息直接定位到缺失列 |
| AI 修复方案 | ALTER TABLE 补充缺失列 |
| 方案是否有效 | 是，但引发了 Bug #1（ALTER 导致 MySQL 崩溃） |
| 深层问题 | AI 修复了表面问题（加列），但没有识别出两个 SQL 脚本不同步的根本架构问题——应该统一使用一个迁移工具（如 Flyway）而不是维护多个 SQL 文件 |

**Bug #3：订单 JOIN 导致数据不可见**

| 维度 | 详情 |
|------|------|
| 问题现象 | 接单后"我接取的"列表查不到 |
| 根因 | 订单查询中使用 `JOIN sys_user`（INNER JOIN），用户记录丢失后订单也无法展示 |
| AI 是否准确定位 | 是。抽查数据库发现用户记录不存在，推理出 INNER JOIN 失效 |
| AI 修复方案 | 改为 LEFT JOIN，增加查询健壮性 |
| 方案是否有效 | 是。即使关联用户数据异常，订单仍可正常显示 |

**Bug #4：中文数据乱码**

| 维度 | 详情 |
|------|------|
| 问题现象 | 插入的中文测试数据全部变成 `?????????` |
| 根因 | bash → docker exec → mysql 命令链中 UTF-8 编码被转换为 Latin-1 |
| AI 修复方案 | 先尝试 `--default-character-set=utf8mb4` 参数 → 失败；改为通过 heredoc 在容器内部写 SQL 文件再 `mysql < file` |
| 方案是否有效 | 最终方案有效 |

**AI 调试总结**：
- AI 擅长：分析日志报错、数据库元数据检查、SQL 语法问题定位
- AI 较弱的：跨组件关联问题（MySQL 崩溃对后端连接池的连锁影响）、深层架构问题（两个 SQL 脚本不同步的根本原因）、编码转换这类环境相关的问题需要多次尝试

---

## 4. Prompt 改进计划（Q3）

**上阶段（P3 详细设计）改进措施的验证结果**：P3 中提出"在 Prompt 中强制区分 Entity 与 Service、提前进行 SOLID 自查"。本阶段编码开发遵循了这些原则，后端代码按 auth/profile/requirement/order/evaluation/notification 模块分包，每个模块含 controller/service/dto，实体类保持纯数据属性。说明该改进措施已固化为团队开发规范。

**编码阶段的 Prompt 最佳实践**：
1. **指定文件路径和行号** — "修改 `OrderService.java:288` 行"比"修改订单查询"精确得多
2. **提供前后文** — 在要求 AI 修改代码前，先让 AI 读取相关文件，避免凭空生成
3. **要求同步编写测试** — 功能改动 + 测试用例一次性交付，避免后续补测试的遗漏

**部署阶段的 Prompt 最佳实践**：
1. **给出完整约束** — 服务器配置、公网 IP、Docker 版本、端口要求一次性告知
2. **明确不做什么** — 如"不需要 SSL 证书""不需要域名"可以减少无用输出
3. **要求可执行交付物** — 不是"写一份 Nginx 配置建议"，而是"写一个可以直接 COPY 到容器的 nginx.conf"

**下阶段计划改进**：
1. 在功能开发阶段就考虑部署需求（如在设计阶段就规划好 Docker 网络和服务名）
2. 建立本地测试 → 服务器部署的自动化流水线，减少手动操作
3. 统一数据库迁移方案（Flyway/Liquibase），消除多个 init.sql 不同步的风险

---

## 5. 本阶段的核心工程判断

**决策内容**：采用 Docker Compose 全容器化部署，Nginx 作为统一入口，后端/数据库/Redis 仅内网可见。

**为什么 AI 无法单独做这个决策**：
1. AI 不知道服务器是阿里云 ECS，安全组只能开放有限端口
2. AI 不了解课程演示场景只需要 HTTP（不需要 HTTPS/域名）
3. AI 不会评估 2 vCPU 4 GiB 跑 4 个容器的资源是否够用（需要人工基于 JVM 内存、MySQL buffer pool、Redis 开销进行估算）
4. AI 无法判断部署脚本的可维护性——团队成员都是学生，需要简洁直观的操作方式

**团队最终如何判断和取舍**：
- 选择 Docker Compose 而非 Kubernetes（后者对于 4 个服务的项目过度复杂）
- 放弃域名和 SSL（课程演示不需要，节省备案时间）
- 选择 `bash deploy.sh` 而非 CI/CD 自动部署（团队成员需要理解部署过程，黑盒自动化不利于学习）
- 保留了本地 docker-compose.yml 和新的 docker-compose-prod.yml 两份编排文件，分别服务开发和线上场景

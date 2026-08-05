# CampusHub AI4SE Final Project

CampusHub 是一个面向大学校园场景的互助服务平台，用统一的 Web 应用承接快递代取、学习求助、资料共享、二手交易、组队招募等轻量互助需求。用户可以注册登录、发布需求、浏览和筛选需求、接单、完成订单、互相评价，并通过信用分和消息通知降低微信群/QQ群互助信息碎片化带来的匹配与信任成本。

本仓库按 AI4SE 期末项目 B 类“非 harness 应用类项目”整理提交。项目不包含自主多轮 agent 主循环，也不调用外部付费 LLM API；本次提交重点展示一个可运行、可测试、可容器分发的真实应用，以及使用 AI 辅助完成需求、计划、实现、验证、评审和反思的工程闭环。

## 功能模块

- 用户与认证：注册、登录、JWT 鉴权、个人资料查看与修改。
- 需求大厅：需求发布、列表分页、分类筛选、关键字搜索、详情查看。
- 订单流程：接单、提交验收、确认完成、取消、参与者权限校验。
- 评价信用：完成订单后双方评价，评分影响用户信用分。
- 消息通知：注册、需求、订单、评价等事件的站内通知与未读管理。
- 管理后台：管理员查看用户、需求、订单，并下架违规需求。

## 技术栈

- 前端：Vue 3、Vue Router、Element Plus、Vite。
- 后端：Spring Boot 3.3、MyBatis-Plus、Spring Security Crypto、JWT。
- 数据：MySQL 8、Redis 7；测试环境使用 H2。
- 分发：Docker Compose，生产演示由 Nginx 提供前端静态资源并代理后端 API。

## 本地开发运行

前置依赖：

- JDK 17
- Maven 3.8+
- Node.js 20 LTS（Node.js 18 也可运行；CI 使用 20）
- Docker Desktop / Docker Compose v2

启动基础设施：

```bash
docker compose up -d
```

本地 `docker-compose.yml` 暴露端口：

```text
MySQL: localhost:3307
Redis: localhost:6379
```

启动后端。前端开发代理当前指向 `localhost:8092`，因此本地开发后端统一跑在 `8092`：

```bash
cd backend
SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3307/campushub_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true' \
SPRING_DATASOURCE_USERNAME='ch_dev' \
SPRING_DATASOURCE_PASSWORD='ch_password' \
SPRING_DATA_REDIS_HOST='localhost' \
SPRING_DATA_REDIS_PORT='6379' \
mvn spring-boot:run -Dspring-boot.run.arguments='--server.port=8092'
```

启动前端：

```bash
cd frontend
npm ci
npm run dev
```

访问地址：

```text
前端页面: http://localhost:5173/
后端健康检查: http://localhost:8092/api/v1/health
```

演示账号建议通过注册页创建。若需要快速演示，可注册：

```text
demo_pub / pass123456
demo_rec / pass123456
```

管理员演示账号需要在数据库中准备。首次启动后可执行以下 SQL 将初始化管理员 `test_admin` 的密码设置为 `pass123456`：

```bash
docker exec campushub-mysql mysql -u ch_dev -pch_password campushub_db \
  -e 'UPDATE sys_user SET password_hash="$2y$10$jb97oCVlvfcHKe9rjrswwuG35Md5TF3ZG941SL5.84uj6OHXzgkw6" WHERE username="test_admin";'
```

数据库初始化方式：

- 本地 MySQL 容器首次启动时，`mysql-init/init.sql` 会自动执行。
- 后端测试不依赖本地 MySQL；测试 profile 使用 `backend/src/test/resources/schema-h2.sql` 初始化 H2 内存数据库。

## 测试与构建

后端单元与集成测试：

```bash
cd backend
mvn test
```

前端生产构建：

```bash
cd frontend
npm ci
npm run build
```

容器构建验证：

```bash
docker compose -f docker-compose-prod.yml build
```

GitHub Actions 配置见 `.github/workflows/ci.yml`，其中 `unit-test` job 会运行后端测试；`frontend-build` job 会运行前端依赖安装和构建；`docker-build` job 会验证生产镜像可构建。`.gitlab-ci.yml` 保留同等检查项，作为原学校 GitLab 提交要求的兼容配置。

## Docker 分发

复制示例环境变量文件并修改生产密钥：

```bash
cp .env.example .env
```

`.env` 是明文文件，只能用于本地或受控部署环境，不应提交到 Git。生产环境必须至少替换：

```text
CAMPUSHUB_JWT_SECRET
DB_USERNAME
DB_PASSWORD
DB_ROOT_PASSWORD
```

`docker-compose-prod.yml` 会强制检查这些变量；缺失时生产容器不会启动。

构建并启动完整生产演示环境：

```bash
docker compose --env-file .env -f docker-compose-prod.yml build
docker compose --env-file .env -f docker-compose-prod.yml up -d
```

访问：

```text
http://localhost/
```

停止：

```bash
docker compose --env-file .env -f docker-compose-prod.yml down
```

生产容器启动后可检查：

```bash
curl http://localhost/api/v1/health
```

预期返回后端健康状态 JSON。前端入口为 `http://localhost/`。

## 安全边界

- 本项目不调用外部 LLM 或付费 API，因此没有用户侧 LLM API Key 存储需求。
- 需要保护的敏感信息包括 JWT secret、数据库密码、Redis 连接信息和生产 `.env`。
- 示例配置仅用于说明变量名，生产部署必须通过环境变量或 `.env` 提供强密码和长随机 JWT secret。
- `.env` 是明文配置，不能进入 Git；查看配置状态时不得回显真实密钥。
- JWT 用于接口鉴权，受保护 API 需要 `Authorization: Bearer <token>`。
- 当前没有实现短信、邮箱验证和真实支付，不能作为生产商业平台直接上线。

## 目录结构

```text
backend/                 Spring Boot 后端服务
frontend/                Vue 3 前端应用与 Nginx 配置
mysql-init/              MySQL 初始化脚本
docker-compose.yml       本地 MySQL/Redis 基础设施
docker-compose-prod.yml  生产演示 Compose
SPEC.md                  AI4SE 项目规约
PLAN.md                  AI4SE 实现计划
SPEC_PROCESS.md          规约与冷启动过程记录
AGENT_LOG.md             AI 协作过程日志
REFLECTION.md            反思报告提交候选稿
```

## AI4SE 提交说明

本项目按个人 AI4SE 期末项目提交整理。历史上该代码库曾包含团队课程过程材料，本次整理已移除旧 P0-P4 文档与生成附件，保留可运行源码、测试、CI、容器分发配置和 AI4SE 要求的过程文档。由于学校 GitLab CI 环境存在非项目原因的不稳定，本项目迁移到 GitHub 后以 GitHub Actions 最新运行结果作为主要 CI 凭证。

线上部署 URL：

```text
待填写：提交前部署到公网后在此补充访问地址。
```

## 已知限制

- 本地开发端口以当前前端代理为准：前端 `5173`，后端 `8092`。
- 生产 Compose 默认监听宿主机 `80` 端口；若端口被占用，需要调整 `docker-compose-prod.yml` 的 Nginx 端口映射。
- 初始化数据中的部分旧账号密码可能是占位哈希，演示时建议通过注册页创建新账号。
- 反思报告 `REFLECTION.md` 已整理为候选稿，最终提交前仍需要本人确认真实经历并标注 AI 辅助范围。

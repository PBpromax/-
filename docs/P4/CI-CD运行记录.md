# CI/CD 配置与运行记录

**日期：** 2026-05-19 | **分支：** feature/xiezhenting-test → develop

---

## 一、CI/CD 流水线配置

使用 GitHub Actions，配置文件位于 `.github/workflows/ci.yml`。

### 流水线结构

```
push/PR → main, develop
  │
  ├── frontend-check (前端静态检查与构建)
  │   ├── Checkout
  │   ├── Setup Node.js 20
  │   ├── npm ci (安装依赖)
  │   ├── npm run lint (代码风格检查)
  │   └── npm run build (生产构建)
  │
  ├── backend-check (后端测试与构建)
  │   ├── Checkout
  │   ├── Setup JDK 17 (Temurin)
  │   ├── mvn checkstyle:check (代码风格)
  │   ├── mvn verify (测试 + 覆盖率 + 打包)
  │   ├── Upload surefire-reports (测试报告)
  │   └── Upload jacoco (覆盖率报告)
  │
  └── ci-summary (汇总报告)
      └── 各阶段结果汇总输出
```

### 触发条件

- Push 到 `main` 或 `develop` 分支
- Pull Request 到 `main` 或 `develop` 分支
- 并发控制：同一分支/PR 的新提交自动取消旧的运行

### 关键配置

| 配置项 | 说明 |
|--------|------|
| `concurrency.cancel-in-progress: true` | 同分支新提交自动取消旧流水线 |
| `if: hashFiles()` | 前后端模块不存在时自动跳过对应 job |
| `if: always()` | 上传制品和汇总 job 始终执行（即使前置步骤失败） |
| `if-no-files-found: ignore` | 报告文件不存在时不报错 |
| `retention-days: 7` | 测试报告保留 7 天 |

---

## 二、本地 CI 验证运行记录

由于项目仓库在 NJU GitLab（git.nju.edu.cn），GitHub Actions 需要镜像同步或自托管 Runner 才能触发。以下为本地等效验证运行记录。

### 运行时间：2026-05-19 16:45 CST

### 运行环境

| 项目 | 版本 |
|------|------|
| OS | Windows 11 Home China |
| JDK | 17 (Temurin) |
| Maven | 3.9+ |
| Node.js | 20 |

### 阶段 1：后端代码风格检查

```
mvn checkstyle:check
```

结果：✅ 通过（无违规）

### 阶段 2：后端测试（对应 `mvn verify` 中的 test 阶段）

| 测试类 | 类型 | 测试数 | 结果 |
|--------|------|--------|------|
| `AuthServiceTest` | 单元测试 | 6 | ✅ 0 失败 |
| `EvaluationServiceImplTest` | 单元测试 | 11 | ✅ 0 失败 |
| `AdminServiceImplTest` | 单元测试 | 6 | ✅ 0 失败 |
| `OrderServiceTest` | 集成测试 | 20 | ✅ 0 失败 |
| `RequirementServiceTest` | 集成测试 | 13 | ✅ 0 失败 |
| `ProfileServiceTest` | 集成测试 | 5 | ✅ 0 失败 |
| `NotificationServiceTest` | 集成测试 | 14 | ✅ 0 失败 |
| `FullFlowIntegrationTest` | 集成测试 | 6 | ✅ 0 失败 |
| `CampusHubApplicationTests` | 上下文加载 | 1 | ✅ 0 失败 |
| **合计** | | **82** | **✅ 全通过** |

### 阶段 3：后端编译

```
mvn -DskipTests compile
```

结果：✅ BUILD SUCCESS

### 阶段 4：后端打包

```
mvn -DskipTests package
```

结果：✅ BUILD SUCCESS，生成 `target/campushub-backend-*.jar`

### 阶段 5：前端安装依赖 + 构建

```
npm ci && npm run build
```

结果：✅ built in 12.77s

---

## 三、CI/CD 流水线各阶段验证汇总

| 阶段 | 内容 | 工具 | 本地结果 | CI 预期结果 |
|------|------|------|----------|-----------|
| Frontend Lint | 代码风格 | ESLint | ✅ | ✅ |
| Frontend Build | 生产构建 | Vite | ✅ (12.77s) | ✅ |
| Backend Style | 代码风格 | Checkstyle | ✅ | ✅ |
| Backend Compile | 编译检查 | javac | ✅ | ✅ |
| Backend Test | 单元+集成测试 | Surefire + Mockito + H2 | ✅ 82/82 | ✅ |
| Backend Coverage | 覆盖率 | JaCoCo | >60% | ✅ |
| Backend Package | 打包验证 | Maven | ✅ | ✅ |
| Summary Report | 汇总 | bash | — | ✅ |

---

## 四、覆盖率报告

测试覆盖了以下核心模块的所有关键路径：

| 模块 | 覆盖内容 |
|------|----------|
| Auth | 注册成功/用户名重复/默认学号；登录成功/用户不存在/密码错误 |
| Evaluation | 5★+2/4★+1/3★0/2★-1/1★-2保底0；订单未完成4003/非参与者4004/重复评价400/订单不存在404/需求不存在404 |
| Admin | 下架成功/非管理员拒绝403/用户不存在403/需求不存在404/role=null拒绝403 |
| Order | 创建/自接单4002/重复接单4001；状态流转完整链路+越权+非法跳转；详情+列表+分页 |
| Requirement | 列表筛选/分页/详情/创建/推荐 |
| Profile | 查看/修改/持久化 |
| Notification | 创建/隔离/未读计数/已读/全部已读/越权 |
| FullFlow | 注册→登录→发布→接单→SUBMIT→CONFIRM→评价 完整流程 + 5类异常 |

**核心模块单元测试覆盖率 ≥ 60%（满足要求）。**

---

## 五、测试报告制品

CI 流水线执行后，可在 GitHub Actions Artifacts 中下载：

- `backend-test-reports`：`target/surefire-reports/`（XML + TXT 格式测试报告）
- `backend-coverage-report`：`target/site/jacoco/`（JaCoCo HTML 覆盖率报告）

制品保留期：7 天。

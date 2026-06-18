# NexEval 项目指南

本文档面向参与本仓库开发、维护和自动化修改的开发者与 AI 编码代理。内容以当前代码、配置和数据库脚本为准；当 README 或设计文档与实现不一致时，应优先相信代码和运行配置。

## 1. 项目概览

NexEval 是一个 B/S 架构的在线考试与智能测评系统，主要覆盖：

- 学生练习、正式考试和 CAT（计算机自适应测验）；CAT 题池仅使用 `CHOICE` 选择题。
- 客观题自动判分、主观题人工阅卷与 AI 辅助阅卷。
- 教师组卷、题库检索、班级与成绩查询。
- 学生成绩复核申请和管理员审核。
- 基于 AI 的薄弱知识点分析、讲解、训练和评估。
- 用户、课程、教学班、教师权限及批量数据导入。

系统当前支持 `student`、`teacher`、`admin` 三类用户。主要业务请求通过 WebSocket 完成，文件上传、密码重置和部分 AI 功能使用 REST 接口。

## 2. 技术栈

### 前端

- Vue 3.5，使用 Composition API 和 `<script setup>`。
- Vite 6，开发环境启用本地 HTTPS。
- Vue Router 4。
- Element Plus 2。
- ECharts 6。
- Cropper.js，用于头像等图片裁剪。
- 原生 WebSocket 封装，未使用 Axios 作为主要业务通道。

### 后端

- Java 21。
- Spring Boot 3.3.4，Gradle 8 构建。
- Spring Web、Validation、WebSocket、Actuator。
- Spring Data JPA，生产环境使用 MySQL 8。
- Spring Data Redis，目前用于密码重置令牌等临时数据。
- Spring Security Crypto，负责 bcrypt 密码处理；项目尚未采用完整的 Spring Security/JWT 登录体系。
- Apache POI，用于 Excel 批量导入。
- 阿里云 DashScope SDK，用于 AI 阅卷和智能分析。

### 基础设施

- MySQL 8.4。
- Redis 7.2。
- Nginx 1.27。
- Docker Compose。
- 本地后端默认使用 HTTPS/HTTP2，容器内后端使用 HTTP 并由 Nginx 代理。

## 3. 目录结构

```text
NexEval/
├─ client/                    Vue 前端
│  ├─ src/views/              页面组件
│  ├─ src/components/         可复用业务组件
│  ├─ src/services/           REST/AI 服务封装
│  ├─ src/router.js           路由与角色访问控制
│  ├─ src/ws.js               WebSocket 请求响应封装
│  └─ src/auth.js             浏览器端登录状态
├─ server/                    Spring Boot 后端
│  ├─ src/main/java/com/nexeval/
│  │  ├─ controller/          REST 控制器
│  │  ├─ ws/                  WebSocket 入口与连接管理
│  │  ├─ service/             业务逻辑
│  │  ├─ repository/          JPA Repository
│  │  ├─ model/               JPA 实体和领域模型
│  │  ├─ dto/                 请求与响应对象
│  │  └─ config/              WebSocket、静态资源和安全响应头配置
│  ├─ avatar/                 默认头像及本地自定义头像
│  ├─ fig/                    内置题目图片
│  ├─ question-images/        上传的题目图片
│  └─ answer-images/          上传的主观题答案图片，运行时生成且已忽略
├─ docs/
│  ├─ database/               建表、升级和批量导入 SQL
│  └─ frontend/               前端设计系统和目标规范
├─ infra/                     Dockerfile、Compose、Nginx、MySQL 配置
├─ image/                     README 使用的截图资源
└─ *.xlsx                     批量导入模板/样例数据
```

根目录的 `students.xlsx`、`teachers.xlsx`、`courses.xlsx`、`classes.xlsx` 和 `SClist.xlsx` 分别对应学生、教师、课程、教学班及选课成绩数据。

## 4. 前端结构与页面

入口文件是 `client/src/main.js`，路由定义在 `client/src/router.js`。

当前主要路由：

| 路由 | 页面 | 访问条件 |
| --- | --- | --- |
| `/login` | 登录 | 未登录用户 |
| `/register` | 注册 | 未登录用户 |
| `/forgot-password` | 找回密码 | 未登录用户 |
| `/reset-password` | 重置密码 | 未登录用户 |
| `/` | 学生/教师主页 | 已登录，管理员会跳转到管理页 |
| `/admin` | 管理员页面 | 管理员 |
| `/exam/:sessionId` | 普通练习或考试答题 | 已登录非管理员 |
| `/cat/:sessionId` | CAT 自适应答题 | 已登录非管理员 |
| `/cat/weakness` | CAT 薄弱点分析 | 已登录非管理员 |
| `/grading` | 教师阅卷 | 教师 |

登录状态由 `client/src/auth.js` 在浏览器端维护，路由守卫根据登录用户的 `type` 做页面跳转。当前不是 JWT 鉴权模式，不能把 `docs/frontend/ApiCollaboration.md` 中的 JWT 描述当成已实现行为。

`client/src/ws.js` 提供 WebSocket 客户端：

- 自动根据页面协议选择 `ws` 或 `wss`。
- 默认连接 `/ws/exam`，考试会话可附带 `sessionId` 查询参数。
- 通过 `request(action, payload, timeoutMs)` 发送请求并按 `requestId` 匹配响应。
- 默认请求超时 10 秒。
- 每 30 秒发送一次 `PING` 心跳。
- 连接关闭时拒绝全部未完成请求。

## 5. 后端结构与核心业务

后端入口为 `com.nexeval.NexEvalApplication`。

### 分层职责

- `controller`：仅承载少量必须使用 HTTP 的接口，如文件上传、密码重置和 AI 薄弱点服务。
- `ws.ExamWebSocketHandler`：主要业务入口，解析 WebSocket action 并分发到 service。
- `service`：登录、考试会话、CAT、班级查询、题库、AI、管理端等业务逻辑。
- `repository`：Spring Data JPA 数据访问。
- `model`：数据库实体、联合主键和会话领域对象。
- `dto`：跨层和网络边界的数据结构。
- `config`：异常处理、静态资源、WebSocket、安全响应头及密码组件。

### 主要服务

- `UserAuthService`：登录、资料修改、头像、用户注册。
- `CatExamService`：练习、考试、CAT 会话、答题、评分、阅卷、复核和报告。
- `IrtCatService`：CAT/IRT 选题与能力估计。
- `ClassQueryService`：教师班级、学生课程、成员、成绩分布和分层名单。
- `AdminManagementService`：课程、教学班、批量导入及教师权限。
- `QuestionBankManagementService`：题库创建和 AI 草稿。
- `AiGradingService`、`AiWeaknessService`、`AiPracticeService`：AI 阅卷与薄弱点训练。
- CAT 智能评估简报必须按 `sessionId` 从后端读取真实逐题作答记录后生成，不得只依赖前端汇总指标。
- CAT 知识点百分比优先从题干 `【知识点：...】` 标签按实际答对数/题数计算；缺少明确标签时的 AI 结果必须标记为推断值。
- `PasswordResetService`、`EmailService`：邮件找回密码和临时令牌。

### 角色能力

学生：

- 查看个人资料、课程、成绩和可参加的试卷。
- 发起练习、正式考试或 CAT 测评。
- 提交文本/选项答案，主观题可附带图片。
- 查看历史记录、自动评分结果和 AI 评价。
- 发起成绩复核，查看薄弱知识点与训练结果。

教师：

- 查看任教课程、学生名单和成绩分布。
- 检索题库、创建试卷并发布给教学班。
- 在 `/grading` 查看答题记录并人工阅卷。
- VIP 教师可使用 AI 阅卷；后端也会校验 VIP 权限。
- 组卷能力受 `can_create_exam` 权限控制。

管理员：

- 注册用户，创建课程和教学班。
- 从 Excel 批量导入基础数据。
- 管理教师 VIP 和组卷权限。
- 审核成绩复核申请。

## 6. WebSocket 协议

端点：

```text
/ws/exam
/ws/exam?sessionId={sessionId}
```

请求格式：

```json
{
  "type": "REQUEST",
  "requestId": "唯一请求编号",
  "action": "LOGIN",
  "payload": {}
}
```

成功响应：

```json
{
  "type": "RESPONSE",
  "requestId": "唯一请求编号",
  "action": "LOGIN",
  "success": true,
  "payload": {}
}
```

失败响应：

```json
{
  "type": "RESPONSE",
  "requestId": "唯一请求编号",
  "action": "LOGIN",
  "success": false,
  "error": {
    "message": "错误信息"
  }
}
```

服务端事件使用 `type: "EVENT"`，目前包括 `CONNECTED`、`PONG`、`ANSWER_UPDATED` 和 `ERROR`。

主要 action 按领域划分如下：

- 登录与资料：`LOGIN`、`UPDATE_PROFILE`、`UPDATE_AVATAR`、`RESET_AVATAR`。
- 管理端：`REGISTER_USER`、`CREATE_COURSE`、`CREATE_CLASS`、`IMPORT_BATCH`。
- 教师权限：`GET_TEACHER_VIPS`、`UPDATE_TEACHER_VIP`、`GET_TEACHER_EXAM_PERMS`、`UPDATE_TEACHER_EXAM_PERM`。
- 班级与成绩：`GET_TEACHER_CLASSES`、`GET_STUDENT_CLASSES`、`GET_COURSE_MEMBERS`、`GET_SCORE_DISTRIBUTION`、`GET_SCORE_TIER_STUDENTS`、`SEARCH_COURSE_SCORES`。
- 题库与组卷：`SEARCH_QUESTIONS`、`CREATE_EXAM_PAPER`、`GET_AVAILABLE_PAPERS`。
- 会话：`START_SESSION`、`START_PRACTICE`、`START_CAT`、`START_EXAM`、`GET_SESSION_STATE`、`GET_SESSION_ANSWERS`、`FINISH_SESSION`。
- 答题：`GET_EXAM_QUESTIONS`、`NEXT_QUESTION`、`SUBMIT_ANSWER`。
- 记录与阅卷：`GET_EXAM_ATTEMPTS`、`GET_ATTEMPT_ANSWERS`、`REVIEW_ANSWER`、`AI_REVIEW_ANSWER`、`STUDENT_AI_REVIEW_ANSWER`。
- 成绩复核：`CREATE_SCORE_APPEAL`、`GET_SCORE_APPEALS`、`REVIEW_SCORE_APPEAL`。
- CAT 报告：`GENERATE_CAT_REPORT`、`GENERATE_CAT_KNOWLEDGE_INSIGHTS`。

新增 WebSocket 业务时，应保持现有协议，在 `ExamWebSocketHandler` 中增加 action 分发，并优先把复杂逻辑放到对应 service，而不是堆积在 handler 中。

## 7. REST 与静态资源接口

当前 REST 接口主要包括：

- `POST /api/avatar/{userId}`：上传头像。
- `DELETE /api/avatar/{userId}`：恢复默认头像。
- `POST /api/answer-image/upload`：上传主观题答案图片。
- `POST /api/question-image/upload`：上传题目图片。
- `POST /api/question-bank`：创建题目。
- `POST /api/question-bank/ai-draft`：生成题目 AI 草稿。
- `POST /api/password-reset/forgot`：发送密码重置邮件。
- `POST /api/password-reset/reset`：执行密码重置。
- `/api/ai/weakness/*`：薄弱点题目、讲解、训练和评估。

静态资源路径包括 `/avatar/**`、`/fig/**`、`/question-images/**` 和 `/answer-images/**`。增加新上传目录时，需要同时检查：

1. Spring 静态资源映射。
2. Vite 开发代理。
3. Nginx 生产代理。
4. Docker 卷挂载和目录写权限。

## 8. 数据模型与持久化

生产数据库 schema 为 `sedb`，JPA 配置为 `ddl-auto: none`，因此表结构必须通过 `docs/database/` 下的 SQL 管理。

基础教学数据：

- `users`：统一用户账户。
- `student`、`teacher`：角色资料。
- `course`：课程。
- `class`：课程与教师组成的教学班。
- `sc`：学生选课与成绩。

考试与题库数据：

- 普通、判断、填空、主观题题库及对应试卷关系表。
- `exam_attempt`：一次考试/练习/CAT 尝试。
- `exam_answer`：逐题作答、评分和阅卷信息。
- `paper_publish`：试卷发布关系。
- `paper_question_item`：统一试卷题目项。
- `judge_question_media`：判断题媒体信息。
- `score_appeal`：成绩复核流程。

`SessionMode` 包含 `PRACTICE`、`CAT`、`EXAM`。进行中的 `ExamSession` 和部分试题缓存由 `CatExamService` 使用 `ConcurrentHashMap` 保存在内存中；考试记录、答案、阅卷和复核结果持久化到 MySQL。因此后端重启可能丢失尚未完成的内存会话，修改会话恢复策略时要特别注意这一点。

Redis 已接入，但当前不是所有会话的统一缓存层，主要可见用途是密码重置令牌和频率限制。

### 数据库初始化

Docker 首次创建 MySQL 数据卷时，会按文件名顺序执行挂载到 `/docker-entrypoint-initdb.d` 的 `docs/database/*.sql`。`z99_batch_import.sql` 负责串联批量导入数据。

初始化脚本只会在空数据卷上自动执行。修改 SQL 后如需完全重建本地数据库：

```powershell
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d --build
```

注意：`down -v` 会删除本地数据库卷和其中全部数据，只能在确认允许重置数据时使用。

## 9. 本地开发

### 前置条件

- Node.js 20 或兼容版本。
- Java 21。
- Gradle 8.x（仓库当前没有 Gradle Wrapper）。
- MySQL 8；密码重置功能还需要 Redis。
- 如使用完整容器方案，需要 Docker Desktop。

### 启动后端

首次运行需要准备本地 PKCS12 证书，仓库 README 提供了 `keytool` 示例。常用环境变量：

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<本地数据库密码>"
$env:SSL_ENABLED = "true"
$env:SSL_KEY_STORE = "file:./certs/nexeval.p12"
$env:SSL_KEY_STORE_PASSWORD = "<证书密码>"
$env:ALLOWED_ORIGINS = "https://localhost:5173"
cd server
gradle bootRun
```

本地后端默认地址为 `https://localhost:8443`，健康检查为 `/actuator/health`。

AI 和邮件功能还依赖 DashScope、SMTP 等环境配置。开发普通业务时不要在代码或文档中新增真实凭据。

### 启动前端

```powershell
cd client
npm install
npm run dev
```

开发地址为 `https://localhost:5173`。Vite 会把 `/ws`、静态图片路径及已配置的 `/api/*` 路径代理到 `https://localhost:8443`，并忽略本地开发证书校验。

### Docker 启动

```powershell
docker compose -f infra/docker-compose.yml up -d --build
```

默认端口：

- 前端/Nginx：`http://localhost`（80）。
- 后端直连：`http://localhost:8080`。
- MySQL：`localhost:3306`。
- Redis：`localhost:6379`。

容器部署中，前端由 Nginx 托管构建后的静态文件；后端容器关闭 SSL，由 Nginx 转发 WebSocket、上传接口和静态资源。

## 10. 开发约定

### 通用原则

- 修改前先阅读相关页面、service、DTO、实体和 SQL，避免只改网络协议的一端。
- 优先沿用现有代码组织和命名，不为单一场景引入新的框架或全局状态库。
- 不要顺手重构无关模块，也不要覆盖工作区中来源不明的现有改动。
- 数据库结构变更必须提供可重复执行或明确顺序的 SQL，并同步 JPA 实体。
- 新增环境配置时使用环境变量和无敏感信息的默认值。

### 前端

- 使用 Vue 3 Composition API 和 `<script setup>`。
- 页面组件目前承担较多业务状态；新增独立 REST 调用时优先放在 `src/services/`。
- WebSocket 业务统一通过 `src/ws.js` 的 `request` 方法调用。
- 路由权限变化需同步检查学生、教师、管理员三种身份。
- 复用 `src/styles.css` 中的现有视觉语言；`docs/frontend/DesignSystem.md` 可作为参考，但不是所有设计 token 和公共组件都已实现。
- 当前项目未实际接入 Pinia、Axios、ESLint 或 Prettier。不要仅根据规范文档假定这些依赖存在。

### 后端

- 控制器和 WebSocket handler 只负责参数接收、协议转换和业务分发。
- 业务逻辑进入 service，数据访问进入 repository。
- 网络边界优先使用 DTO，不直接暴露可变实体。
- JPA 实体字段、枚举值和 SQL 类型必须保持一致。
- 主观题分数、权限和资源所有权必须在后端校验，不能只依赖前端按钮隐藏。
- 涉及会话状态的修改要同时考虑 `PRACTICE`、`CAT`、`EXAM` 三种模式、超时和重复提交。
- CAT 只从 `question_bank` 加载 `CHOICE` 题目；判断题、填空题和大题不得进入 CAT 题池。新会话优先排除上一轮同课程 CAT 已答题目，题库用尽时才允许复用。
- CAT 选题在 IRT 信息量基础上加入后端难度趋势：答对后优先选择接近“上一题 + 0.5”的更难题，答错后优先选择接近“上一题 - 0.5”的更简单题；目标方向无题时可选同难度题，不得转向相反方向强行出题。
- 文件上传要校验类型、大小、路径归属和文件名，禁止信任客户端提供的磁盘路径。

## 11. 验证与测试

前端构建：

```powershell
cd client
npm ci
npm run build
```

后端测试：

```powershell
cd server
gradle test
```

当前自动化测试只有基础 Spring 上下文加载测试，测试环境使用 H2 的 MySQL 兼容模式。涉及核心业务时，应补充 service 或 repository 测试，不能只依赖 `contextLoads`。

容器检查：

```powershell
docker compose -f infra/docker-compose.yml config
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml logs backend
```

根据改动范围至少人工验证：

- 三种角色的登录和路由跳转。
- WebSocket 连接、心跳、超时与错误响应。
- 练习、正式考试和 CAT 的开始、答题、交卷。
- 阅卷、AI 阅卷权限和成绩复核。
- 图片上传、静态资源访问和容器卷持久化。
- 数据库从空卷初始化是否成功。

## 12. 已知现状与风险

- 自动化测试覆盖很少，前端没有现成测试脚本。
- 部分 Vue 页面体积较大，改动时应控制范围并重点回归相邻功能。
- `README.md`、`client/README.md`、`server/README.md` 中有些功能列表已经落后于代码。
- `docs/frontend/` 中的 Pinia、Axios、JWT、公共组件库等内容包含目标设计，不等于当前已实现架构。
- 进行中的考试会话主要保存在后端内存中，不具备完整的重启恢复和多实例共享能力。
- 登录态与角色跳转主要由客户端维护，后端各业务入口必须独立进行权限和归属校验。
- Docker 数据库初始化依赖文件执行顺序；已有数据卷不会自动应用新脚本。
- Nginx 当前只显式代理部分 `/api/*` 路径。新增 REST 端点后必须确认生产代理配置。

## 13. 安全要求

仓库历史中曾出现硬编码的数据库、SMTP 或第三方 AI 凭据；当前运行配置仅通过环境变量读取这些值：

- 不要在日志、提交信息、测试快照或本文档中复制真实密钥。
- 新代码必须从环境变量读取凭据。
- 应尽快轮换已经进入版本库的凭据，并将仓库默认值替换为无效占位符。
- `.env` 和 `.env.*` 已被忽略，但仍需避免把本地秘密写入其他受版本控制文件。
- Docker 本地配置从 `infra/.env` 读取，仓库只保留无真实凭据的 `infra/.env.example`。
- 修改 CORS、WebSocket allowed origins、TLS 或上传接口时，应按安全变更处理并进行专项验证。

## 14. 信息优先级

发生描述冲突时，按以下顺序判断当前真实行为：

1. 可运行代码和测试。
2. `application.yml`、`vite.config.js`、`docker-compose.yml`、`nginx.conf` 等运行配置。
3. `docs/database/` 中实际执行的 SQL。
4. 根目录及子模块 README。
5. `docs/frontend/` 中的设计与规划文档。

修改实现后，应同步更新本文件中受影响的项目事实，避免后续维护者继续依赖过时信息。

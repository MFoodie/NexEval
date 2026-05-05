# NexEval B/S 在线考试系统

这是一个用于构建 CAT 类在线考试系统的浅层目录 B/S（浏览器/服务器）项目。

## 技术栈

- 前端：Vue 3 + Vite + Element Plus
- 后端：Java 21 + Spring Boot 3（Gradle，WebSocket API）
- 基础设施（可选）：MySQL + Redis + Nginx

## 目录结构

```text
NexEval/
  client/          # 浏览器端应用（Vue）
  server/          # 服务端应用（Spring Boot）
  infra/           # Docker 与 Nginx 示例配置
  .gitignore
  README.md
```

为方便维护，项目顶层目录深度保持较浅。

## 快速启动

### 1. 启动后端

先生成本地 TLS 证书（仅首次需要）：

```powershell
cd server
mkdir certs -ErrorAction SilentlyContinue
keytool -genkeypair -alias nexeval -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore .\certs\nexeval.p12 -validity 3650 -storepass changeit -keypass changeit -dname "CN=localhost, OU=NexEval, O=NexEval, L=Local, ST=Local, C=CN"
```

启动后端：

```powershell
cd server
gradle bootRun
```

后端地址：[https://localhost:8443](https://localhost:8443)

### 2. 启动前端

```powershell
cd client
npm install
npm run dev
```

前端地址（HTTP）：[http://localhost:5173](http://localhost:5173)

前端地址（HTTPS）：[https://localhost:5173](https://localhost:5173)

### 3. 可选：启动本地基础设施

```powershell
cd infra
docker compose up -d
```

## 接口现状

- 业务 REST 端点已移除。
- 考试主流程仅使用 WebSocket 请求-响应。

## 角色功能

- 学生端：
  - 查看个人信息、所属教学班与课程成绩。
  - 按课程发起题目练习/进入考试。
  - 在考试页读取考试剩余时间（`remainingSeconds`）并提交答题。
  - 提交成绩复核申请，并查看当前课程的复核记录。
- 教师端：
  - 查看教学班与学生列表。
  - 查看学生历史考试记录与答题详情。
  - 对主观题进行人工批改（分数会按题目上限自动截断）。
- 管理员端：
  - 用户信息注册、课程信息管理、教学班管理、批量导入。
  - 成绩复核审理（同意时将该次考试主观题分数置 0，拒绝则保留原分）。

## WebSocket 通道

- WS 端点：/ws/exam 或 /ws/exam?sessionId={sessionId}
- 请求动作：
  - 登录/资料：LOGIN、UPDATE_PROFILE、UPDATE_AVATAR、RESET_AVATAR
  - 管理端：REGISTER_USER、CREATE_COURSE、CREATE_CLASS、IMPORT_BATCH
  - 教学班：GET_TEACHER_CLASSES、GET_STUDENT_CLASSES
  - 考试会话：START_SESSION、START_PRACTICE、START_EXAM、GET_EXAM_QUESTIONS、GET_SESSION_STATE、GET_SESSION_ANSWERS、FINISH_SESSION、NEXT_QUESTION、SUBMIT_ANSWER
  - 记录与批改：GET_EXAM_ATTEMPTS、GET_ATTEMPT_ANSWERS、REVIEW_ANSWER
  - 成绩复核：CREATE_SCORE_APPEAL、GET_SCORE_APPEALS、REVIEW_SCORE_APPEAL
- 响应格式：RESPONSE（requestId/action/success/payload）
- 服务端事件：CONNECTED、PONG、ANSWER_UPDATED、ERROR

## 数据库登录（MySQL sedb）

- 后端连接 MySQL 的 sedb schema。
- 支持卡号/手机号/邮箱 + 密码登录。
- 登录成功后，数据库中的密码会以 bcrypt 密文保存。
- 默认头像根据 type + sex 选择，通过 /avatar/** 提供访问：
  - student：student_male.png / student_female.png
  - teacher：teacher_male.png / teacher_female.png
  - admin：admin_male.png / admin_female.png
- 自定义头像支持 JPG/PNG，裁剪为圆形 PNG 后保存为 server/avatar/{cardNo}.png。
- 恢复默认头像会删除对应的自定义 {cardNo}.png 文件。

数据库必需表（最小集合）：

- 基础用户与教学关系：users、student、teacher、course、class、SC
- 考试记录：exam_attempt、exam_answer
- 复核流程：score_appeal

建表脚本位于：`docs/database/`

- `create.sql`（基础表）
- `create_exam.sql`、`create_exam_answer.sql`（考试记录）
- `create_exam_judge.sql`、`create_exam_blank.sql`、`create_exam_essay.sql`（题型题库）
- `create_score_appeal.sql`（成绩复核）

若你的旧库中 class.eid 仍关联 teacher.id，可执行修复脚本：infra/sql/20260426_fix_class_fk.sql

## 批量导入模板

模板文件位于项目根目录（students.xlsx / teachers.xlsx / courses.xlsx / classes.xlsx / SClist.xlsx）。

- students.xlsx：卡号、姓名、性别、学号、入学年份、专业、学院
- teachers.xlsx：卡号、姓名、性别、工号、入职年份、职称、学院
- courses.xlsx：课程编号、课程名称、学分
- classes.xlsx：课程编号、教师工号
- SClist.xlsx：学号、课程编号、教师工号、成绩

使用你的数据库账号启动后端：

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "123456"
$env:SSL_ENABLED = "true"
$env:SSL_KEY_STORE = "file:./certs/nexeval.p12"
$env:SSL_KEY_STORE_PASSWORD = "changeit"
$env:SSL_KEY_ALIAS = "nexeval"
cd server
gradle bootRun
```

## 默认安全加固

- TLS/HTTPS + HTTP/2
- 限制 WebSocket 允许来源（ALLOWED_ORIGINS）
- 安全响应头：HSTS、CSP、X-Frame-Options、X-Content-Type-Options

## 说明

- 当前实现为“会话状态内存 + 关键结果持久化”模式：
  - 会话进行态（如自适应过程）在服务内存维护；
  - 答题记录、考试尝试、复核申请与审理结果持久化到 MySQL。
- MySQL 与 Redis 已预留，后续可继续扩展缓存、并发与审计能力。
- 后续可继续扩展认证、题库管理、考试编排、AI 阅卷、统计分析等模块。

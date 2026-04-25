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

## WebSocket 通道

- WS 端点：/ws/exam 或 /ws/exam?sessionId={sessionId}
- 请求动作：LOGIN、START_SESSION、NEXT_QUESTION、SUBMIT_ANSWER、UPDATE_AVATAR、RESET_AVATAR
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

数据库必需表（你已创建）：users

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

- 当前 CAT 逻辑是内存版实现，用于快速原型验证。
- MySQL 与 Redis 已预留，但部分能力仍可继续扩展。
- 后续可继续扩展认证、题库管理、考试编排、AI 阅卷、统计分析等模块。

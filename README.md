# NexEval B/S Skeleton

A shallow-folder B/S (Browser/Server) scaffold for building a CAT-like online exam system.

## Stack

- Frontend: Vue 3 + Vite + Element Plus
- Backend: Java 21 + Spring Boot 3 (Gradle, WebSocket API)
- Infra (optional): MySQL + Redis + Nginx

## Folder Layout

```text
NexEval/
  client/          # Browser-side app (Vue)
  server/          # Server-side API (Spring Boot)
  infra/           # Docker and Nginx samples
  .gitignore
  README.md
```

The top-level depth is intentionally flat to keep maintenance simple.

## Quick Start

### 1. Start backend API

Generate a local TLS certificate first (one-time):

```powershell
cd server
mkdir certs -ErrorAction SilentlyContinue
keytool -genkeypair -alias nexeval -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore .\\certs\\nexeval.p12 -validity 3650 -storepass changeit -keypass changeit -dname "CN=localhost, OU=NexEval, O=NexEval, L=Local, ST=Local, C=CN"
```

Start backend:

```powershell
cd server
gradle bootRun
```

Backend host: [https://localhost:8443](https://localhost:8443)

### 2. Start frontend app

```powershell
cd client
npm install
npm run dev
```

UI base: [http://localhost:5173](http://localhost:5173)

UI base (HTTPS): [https://localhost:5173](https://localhost:5173)

### 3. Optional local infra

```powershell
cd infra
docker compose up -d
```

## Current API Skeleton

- Business REST endpoints removed.
- Exam flow now uses websocket request-response only.

## WebSocket Channel

- WS endpoint: /ws/exam or /ws/exam?sessionId={sessionId}
- Request actions: LOGIN, START_SESSION, NEXT_QUESTION, SUBMIT_ANSWER
- Response envelope: RESPONSE (requestId/action/success/payload)
- Server events: CONNECTED, PONG, ANSWER_UPDATED, ERROR

## Database Login (MySQL sedb)

- Backend now connects to MySQL schema: sedb
- Login supports: card id / phone / email + password
- Password in DB is stored as bcrypt hash after successful login
- Default avatar is selected by type+sex and served from /avatar/**
  - student: student_male.png / student_female.png
  - teacher: teacher_male.png / teacher_female.png
  - admin: admin_male.png / admin_female.png

Required table (already in your DB): users

Run backend with your DB account:

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

Security hardening enabled by default:

- TLS/HTTPS + HTTP/2
- Restricted WebSocket origins (`ALLOWED_ORIGINS`)
- Security headers: HSTS, CSP, X-Frame-Options, X-Content-Type-Options

## Notes

- The current CAT logic is an in-memory starter implementation for rapid prototyping.
- MySQL and Redis are prepared in infra but not yet wired into persistence/session layers.
- This structure is ready for next-phase modules: auth, question bank management, exam scheduling, AI grading, and analytics.

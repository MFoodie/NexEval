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

```powershell
cd server
gradle bootRun
```

Backend host: [http://localhost:8080](http://localhost:8080)

### 2. Start frontend app

```powershell
cd client
npm install
npm run dev
```

UI base: [http://localhost:5173](http://localhost:5173)

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
- Request actions: START_SESSION, NEXT_QUESTION, SUBMIT_ANSWER
- Response envelope: RESPONSE (requestId/action/success/payload)
- Server events: CONNECTED, PONG, ANSWER_UPDATED, ERROR

## Notes

- The current CAT logic is an in-memory starter implementation for rapid prototyping.
- MySQL and Redis are prepared in infra but not yet wired into persistence/session layers.
- This structure is ready for next-phase modules: auth, question bank management, exam scheduling, AI grading, and analytics.

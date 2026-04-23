# Server Module

## Run

```powershell
mkdir certs -ErrorAction SilentlyContinue
keytool -genkeypair -alias nexeval -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore .\certs\nexeval.p12 -validity 3650 -storepass changeit -keypass changeit -dname "CN=localhost, OU=NexEval, O=NexEval, L=Local, ST=Local, C=CN"
gradle bootRun
```

## Package Overview

- com.nexeval.repository: user account repository (MySQL)
- com.nexeval.service: CAT session logic
- com.nexeval.model: domain entities
- com.nexeval.dto: request/response payloads
- com.nexeval.ws: websocket endpoint and session hub

## WebSocket

- Endpoint: /ws/exam or /ws/exam?sessionId={sessionId}
- Client request action: LOGIN, START_SESSION, NEXT_QUESTION, SUBMIT_ANSWER
- Response: RESPONSE (requestId/action/success/payload|error)
- Events: CONNECTED, PONG, ANSWER_UPDATED, ERROR
- Allowed origins are restricted by `ALLOWED_ORIGINS` (default: https://localhost:5173)

## MySQL Config

- Default schema: sedb
- Env vars: DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD
- Password policy: plain password is accepted for first successful login and then upgraded to bcrypt hash in column users.password

## Next Steps

- Add persistence layer (MySQL + JPA/MyBatis)
- Add JWT auth and role model (student/teacher/admin)
- Add exam event and proctoring integration

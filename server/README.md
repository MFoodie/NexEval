# Server Module

## Run

```powershell
gradle bootRun
```

## Package Overview

- com.nexeval.service: CAT session logic
- com.nexeval.model: domain entities
- com.nexeval.dto: request/response payloads
- com.nexeval.ws: websocket endpoint and session hub

## WebSocket

- Endpoint: /ws/exam or /ws/exam?sessionId={sessionId}
- Client request action: START_SESSION, NEXT_QUESTION, SUBMIT_ANSWER
- Response: RESPONSE (requestId/action/success/payload|error)
- Events: CONNECTED, PONG, ANSWER_UPDATED, ERROR

## Next Steps

- Add persistence layer (MySQL + JPA/MyBatis)
- Add JWT auth and role model (student/teacher/admin)
- Add exam event and proctoring integration

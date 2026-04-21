# Client Module

## Run

```powershell
npm install
npm run dev
```

## Routes

- / : start page
- /exam/:sessionId : CAT question flow

## Real-time Channel

- Browser connects to /ws/exam and /ws/exam?sessionId={sessionId}
- Vite proxies /ws to backend ws://localhost:8080
- Exam business actions go through websocket request-response (no /api calls)

## Next Steps

- Add login and role-aware pages
- Add exam timer and anti-cheat interactions
- Add teacher dashboard and grading workflows

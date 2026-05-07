# Client Module

## Run

```powershell
npm install
npm run dev
```

- Local dev URL: [https://localhost:5173](https://localhost:5173)
- If you run Vite inside Docker, use `npm run dev -- --host 0.0.0.0` so the browser can reach the dev server and HMR websocket.

## Routes

- /login : account login (card/phone/email + password)
- / : dashboard with personal info and exam entrance
- /exam/:sessionId : CAT question flow

## Real-time Channel

- Browser connects to /ws/exam and /ws/exam?sessionId={sessionId}
- Vite proxies /ws to backend wss://localhost:8443
- Exam business actions go through websocket request-response (no /api calls)

## Next Steps

- Add login and role-aware pages
- Add exam timer and anti-cheat interactions
- Add teacher dashboard and grading workflows

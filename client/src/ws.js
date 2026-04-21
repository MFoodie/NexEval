function getSocketProtocol() {
  return window.location.protocol === "https:" ? "wss" : "ws";
}

function buildWsUrl(sessionId) {
  const base = `${getSocketProtocol()}://${window.location.host}/ws/exam`;
  if (!sessionId) {
    return base;
  }

  return `${base}?sessionId=${encodeURIComponent(sessionId)}`;
}

export function createExamSocket(sessionId, handlers = {}) {
  const wsUrl = buildWsUrl(sessionId);
  const socket = new WebSocket(wsUrl);
  const pending = new Map();
  let sequence = 0;

  function rejectAllPending(reason) {
    for (const entry of pending.values()) {
      clearTimeout(entry.timer);
      entry.reject(new Error(reason));
    }
    pending.clear();
  }

  socket.onopen = () => {
    handlers.onOpen?.();
    socket.send(JSON.stringify({ type: "PING" }));
  };

  socket.onclose = (event) => {
    rejectAllPending("WebSocket closed");
    handlers.onClose?.(event);
  };

  socket.onerror = (event) => {
    handlers.onError?.(event);
  };

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);

      if (data.type === "RESPONSE" && data.requestId && pending.has(data.requestId)) {
        const entry = pending.get(data.requestId);
        pending.delete(data.requestId);
        clearTimeout(entry.timer);

        if (data.success) {
          entry.resolve(data.payload);
        } else {
          entry.reject(new Error(data.error?.message || "WebSocket request failed"));
        }
      }

      handlers.onMessage?.(data);
    } catch {
      handlers.onMessage?.({ type: "RAW", payload: event.data });
    }
  };

  return {
    isOpen() {
      return socket.readyState === WebSocket.OPEN;
    },
    request(action, payload = {}, timeoutMs = 10000) {
      if (socket.readyState !== WebSocket.OPEN) {
        return Promise.reject(new Error("WebSocket is not connected"));
      }

      const requestId = `${Date.now()}-${++sequence}`;

      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => {
          pending.delete(requestId);
          reject(new Error(`WebSocket request timeout: ${action}`));
        }, timeoutMs);

        pending.set(requestId, { resolve, reject, timer });

        socket.send(
          JSON.stringify({
            type: "REQUEST",
            requestId,
            action,
            payload
          })
        );
      });
    },
    send(type, payload = {}) {
      if (socket.readyState !== WebSocket.OPEN) {
        return;
      }

      socket.send(JSON.stringify({ type, payload }));
    },
    close() {
      if (socket.readyState === WebSocket.CONNECTING || socket.readyState === WebSocket.OPEN) {
        socket.close();
      }
      rejectAllPending("WebSocket client closed");
    }
  };
}

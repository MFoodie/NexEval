package com.nexeval.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class ExamWebSocketHub {

  private final ObjectMapper objectMapper;
  private final Map<String, Set<WebSocketSession>> channels = new ConcurrentHashMap<>();

  public ExamWebSocketHub(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void register(String sessionId, WebSocketSession wsSession) {
    channels.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet()).add(wsSession);
  }

  public void unregister(String sessionId, WebSocketSession wsSession) {
    Set<WebSocketSession> bucket = channels.get(sessionId);
    if (bucket == null) {
      return;
    }

    bucket.remove(wsSession);
    if (bucket.isEmpty()) {
      channels.remove(sessionId);
    }
  }

  public void pushToExam(String sessionId, Map<String, Object> payload) {
    Set<WebSocketSession> bucket = channels.get(sessionId);
    if (bucket == null || bucket.isEmpty()) {
      return;
    }

    String json = toJson(payload);

    for (WebSocketSession wsSession : bucket) {
      sendText(wsSession, json);
    }
  }

  public void pushToClient(WebSocketSession wsSession, Map<String, Object> payload) {
    sendText(wsSession, toJson(payload));
  }

  private String toJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize websocket payload.", ex);
    }
  }

  private void sendText(WebSocketSession wsSession, String jsonText) {
    if (!wsSession.isOpen()) {
      return;
    }

    try {
      synchronized (wsSession) {
        wsSession.sendMessage(new TextMessage(jsonText));
      }
    } catch (IOException ignored) {
      // Broken socket should not break exam flow.
    }
  }
}

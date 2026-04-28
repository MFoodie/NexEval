package com.nexeval.ws;

import com.nexeval.dto.AnswerRequest;
import com.nexeval.service.AdminManagementService;
import com.nexeval.service.CatExamService;
import com.nexeval.service.ClassQueryService;
import com.nexeval.service.UserAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ExamWebSocketHandler extends TextWebSocketHandler {

  private static final String EXAM_SESSION_ID_ATTR = "examSessionId";

  private final ExamWebSocketHub hub;
  private final ObjectMapper objectMapper;
  private final CatExamService catExamService;
  private final UserAuthService userAuthService;
  private final AdminManagementService adminManagementService;
  private final ClassQueryService classQueryService;

  public ExamWebSocketHandler(
    ExamWebSocketHub hub,
    ObjectMapper objectMapper,
    CatExamService catExamService,
    UserAuthService userAuthService,
    AdminManagementService adminManagementService,
    ClassQueryService classQueryService
  ) {
    this.hub = hub;
    this.objectMapper = objectMapper;
    this.catExamService = catExamService;
    this.userAuthService = userAuthService;
    this.adminManagementService = adminManagementService;
    this.classQueryService = classQueryService;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession wsSession) {
    String sessionId = extractSessionId(wsSession.getUri()).orElse(null);

    if (sessionId != null && !sessionId.isBlank()) {
      wsSession.getAttributes().put(EXAM_SESSION_ID_ATTR, sessionId);
      hub.register(sessionId, wsSession);
    }

    Map<String, Object> connected = new LinkedHashMap<>();
    connected.put("type", "EVENT");
    connected.put("event", "CONNECTED");
    connected.put("timestamp", Instant.now().toString());
    if (sessionId != null && !sessionId.isBlank()) {
      connected.put("sessionId", sessionId);
    }
    hub.pushToClient(wsSession, connected);
  }

  @Override
  protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) {
    String sessionId = getSessionId(wsSession);

    try {
      JsonNode incoming = objectMapper.readTree(message.getPayload());
      String type = incoming.path("type").asText("").toUpperCase();

      if ("PING".equals(type)) {
        Map<String, Object> pong = new LinkedHashMap<>();
        pong.put("type", "EVENT");
        pong.put("event", "PONG");
        pong.put("timestamp", Instant.now().toString());
        if (sessionId != null && !sessionId.isBlank()) {
          pong.put("sessionId", sessionId);
        }
        hub.pushToClient(wsSession, pong);
        return;
      }

      if ("REQUEST".equals(type)) {
        handleRequest(wsSession, incoming);
        return;
      }

      hub.pushToClient(wsSession, Map.of(
        "type", "EVENT",
        "event", "ERROR",
        "message", "Unsupported websocket message type"
      ));
    } catch (IOException ignored) {
      hub.pushToClient(wsSession, Map.of(
        "type", "EVENT",
        "event", "ERROR",
        "message", "Invalid websocket message payload"
      ));
    }
  }

  private void handleRequest(WebSocketSession wsSession, JsonNode incoming) {
    String requestId = incoming.path("requestId").asText("").trim();
    String action = incoming.path("action").asText("").trim().toUpperCase();
    JsonNode payload = incoming.path("payload");

    if (requestId.isBlank()) {
      hub.pushToClient(wsSession, Map.of(
        "type", "EVENT",
        "event", "ERROR",
        "message", "requestId is required"
      ));
      return;
    }

    try {
      Object responsePayload;

      switch (action) {
        case "LOGIN":
          responsePayload = userAuthService.login(
            requireText(payload, "account"),
            requireText(payload, "password")
          );
          break;
        case "UPDATE_PROFILE":
          responsePayload = userAuthService.updateProfile(
            requireText(payload, "userId"),
            requireText(payload, "name"),
            requireText(payload, "phone"),
            optionalText(payload, "email"),
            optionalText(payload, "newPassword")
          );
          break;
        case "UPDATE_AVATAR":
          responsePayload = userAuthService.updateAvatar(
            requireText(payload, "userId"),
            requireText(payload, "imageBase64")
          );
          break;
        case "RESET_AVATAR":
          responsePayload = userAuthService.resetAvatar(requireText(payload, "userId"));
          break;
        case "REGISTER_USER":
          responsePayload = userAuthService.registerUser(
            requireText(payload, "id"),
            requireText(payload, "name"),
            requireText(payload, "sex"),
            requireText(payload, "type"),
            optionalText(payload, "sno"),
            optionalText(payload, "studentEnterYear"),
            optionalText(payload, "major"),
            optionalText(payload, "studentDepartment"),
            optionalText(payload, "eid"),
            optionalText(payload, "teacherEnterYear"),
            optionalText(payload, "title"),
            optionalText(payload, "teacherDepartment")
          );
          break;
        case "CREATE_COURSE":
          responsePayload = adminManagementService.createCourse(
            requireText(payload, "cno"),
            requireText(payload, "cname"),
            requireText(payload, "credit")
          );
          break;
        case "CREATE_CLASS":
          responsePayload = adminManagementService.createTeachingClass(
            requireText(payload, "cno"),
            requireText(payload, "eid")
          );
          break;
        case "IMPORT_BATCH":
          responsePayload = adminManagementService.importBatch(
            requireText(payload, "importType"),
            requireText(payload, "fileBase64")
          );
          break;
        case "GET_TEACHER_CLASSES":
          responsePayload = classQueryService.getTeacherClasses(
            requireText(payload, "eid")
          );
          break;
        case "GET_STUDENT_CLASSES":
          responsePayload = classQueryService.getStudentClasses(
            requireText(payload, "sno")
          );
          break;
        case "START_SESSION":
          responsePayload = handleStartSession(payload);
          break;
        case "NEXT_QUESTION":
          responsePayload = catExamService.getNextQuestion(requireText(payload, "sessionId"));
          break;
        case "SUBMIT_ANSWER":
          responsePayload = catExamService.submitAnswer(
            requireText(payload, "sessionId"),
            new AnswerRequest(requireText(payload, "questionId"), requireText(payload, "selectedOption"))
          );
          break;
        default:
          throw new IllegalArgumentException("Unsupported action: " + action);
      }

      hub.pushToClient(wsSession, buildResponse(requestId, action, true, responsePayload, null));
    } catch (RuntimeException ex) {
      String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Request failed" : ex.getMessage();
      hub.pushToClient(wsSession, buildResponse(requestId, action, false, null, message));
    }
  }

  private Object handleStartSession(JsonNode payload) {
    return catExamService.startSession(requireText(payload, "userId"));
  }

  private String requireText(JsonNode payload, String fieldName) {
    String text = payload.path(fieldName).asText("").trim();
    if (text.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return text;
  }

  private String optionalText(JsonNode payload, String fieldName) {
    return payload.path(fieldName).asText("").trim();
  }

  private Map<String, Object> buildResponse(
    String requestId,
    String action,
    boolean success,
    Object payload,
    String message
  ) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("type", "RESPONSE");
    response.put("requestId", requestId);
    response.put("action", action);
    response.put("success", success);

    if (success) {
      response.put("payload", payload);
    } else {
      response.put("error", Map.of("message", message));
    }

    return response;
  }

  @Override
  public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) {
    String sessionId = getSessionId(wsSession);
    if (sessionId != null) {
      hub.unregister(sessionId, wsSession);
    }
  }

  @Override
  public void handleTransportError(WebSocketSession wsSession, Throwable exception) throws Exception {
    String sessionId = getSessionId(wsSession);
    if (sessionId != null) {
      hub.unregister(sessionId, wsSession);
    }
    super.handleTransportError(wsSession, exception);
  }

  private String getSessionId(WebSocketSession wsSession) {
    Object value = wsSession.getAttributes().get(EXAM_SESSION_ID_ATTR);
    return value instanceof String ? (String) value : null;
  }

  private Optional<String> extractSessionId(URI uri) {
    if (uri == null || uri.getRawQuery() == null || uri.getRawQuery().isBlank()) {
      return Optional.empty();
    }

    return Arrays.stream(uri.getRawQuery().split("&"))
      .map(this::splitPair)
      .filter(pair -> pair.length == 2)
      .filter(pair -> "sessionId".equals(pair[0]))
      .map(pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8))
      .findFirst();
  }

  private String[] splitPair(String text) {
    String[] pair = text.split("=", 2);
    if (pair.length == 1) {
      return new String[]{pair[0], ""};
    }
    return pair;
  }
}

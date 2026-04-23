package com.nexeval.config;

import com.nexeval.ws.ExamWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final ExamWebSocketHandler examWebSocketHandler;
  private final String[] allowedOrigins;

  public WebSocketConfig(
    ExamWebSocketHandler examWebSocketHandler,
    @Value("${nexeval.security.allowed-origins:https://localhost:5173}") String allowedOriginsConfig
  ) {
    this.examWebSocketHandler = examWebSocketHandler;
    this.allowedOrigins = allowedOriginsConfig.split("\\s*,\\s*");
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(examWebSocketHandler, "/ws/exam")
      .setAllowedOrigins(allowedOrigins);
  }
}

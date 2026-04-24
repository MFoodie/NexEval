package com.nexeval.config;

import com.nexeval.ws.ExamWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

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

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(4 * 1024 * 1024);
    container.setMaxBinaryMessageBufferSize(4 * 1024 * 1024);
    container.setMaxSessionIdleTimeout(10 * 60 * 1000L);
    return container;
  }
}

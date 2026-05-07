package com.nexeval.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/avatar/**")
      .addResourceLocations("file:./avatar/");

    registry.addResourceHandler("/fig/**")
      .addResourceLocations("file:./fig/");

    registry.addResourceHandler("/question-images/**")
      .addResourceLocations("file:./question-images/");

    registry.addResourceHandler("/answer-images/**")
      .addResourceLocations("file:./answer-images/");
  }
}

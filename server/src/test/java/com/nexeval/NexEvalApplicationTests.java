package com.nexeval;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NexEvalApplicationTests {

  @MockBean
  private JavaMailSender mailSender;

  @Test
  void contextLoads() {
  }
}

package com.nexeval.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private final JavaMailSender mailSender;
  private final String from;

  public EmailService(JavaMailSender mailSender,
                      @Value("${spring.mail.username}") String from) {
    this.mailSender = mailSender;
    this.from = from;
  }

  public void sendResetEmail(String to, String resetUrl) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject("NexEval - 密码重置");

      String html = """
        <div style="max-width:480px;margin:0 auto;padding:24px;font-family:Arial,sans-serif;">
          <h2 style="color:#2c3e50;">NexEval 密码重置</h2>
          <p>您正在申请重置密码，请点击下方按钮完成重置（有效期 15 分钟）：</p>
          <a href="%s"
             style="display:inline-block;margin:16px 0;padding:12px 28px;background:#cf7357;color:#fff;
                    text-decoration:none;border-radius:6px;font-size:16px;">
            重置密码
          </a>
          <p style="color:#999;font-size:13px;">如果按钮无法点击，请复制以下链接到浏览器：<br/>%s</p>
          <p style="color:#999;font-size:13px;">如果您没有申请重置密码，请忽略此邮件。</p>
        </div>
        """.formatted(resetUrl, resetUrl);

      helper.setText(html, true);
      mailSender.send(message);
    } catch (Exception e) {
      throw new IllegalStateException("邮件发送失败，请稍后重试", e);
    }
  }
}

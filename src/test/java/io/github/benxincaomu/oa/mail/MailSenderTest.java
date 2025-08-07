package io.github.benxincaomu.oa.mail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@SpringBootTest
public class MailSenderTest {
    @Resource
    private JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String from;

    @Test
    public void sendMail() throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setTo("wukeleixin@126.com");
        helper.setSubject("HTML邮件123");
        helper.setFrom(from);

        String htmlContent = "<h1>欢迎</h1><p>这是一封HTML邮件</p>";
        helper.setText(htmlContent, true); // 第二个参数true表示HTML内容

        javaMailSender.send(message);
    }

}

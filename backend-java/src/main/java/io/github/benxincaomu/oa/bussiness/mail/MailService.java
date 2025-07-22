package io.github.benxincaomu.oa.bussiness.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class MailService {

    @Resource
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String from;

    public void sendMail(String to, String subject, String content) {
        mailSender.send((message) -> {
           MimeMessageHelper helper = new MimeMessageHelper(message);
           helper.setFrom(from);
           helper.setTo(to);
           helper.setSubject(subject);
           helper.setText(content,true);
        });
    }

}

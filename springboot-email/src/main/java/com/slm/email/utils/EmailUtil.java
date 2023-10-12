package com.slm.email.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmailUtil {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void commonEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        // 收信人
        message.setTo(to);
        // 主题
        message.setSubject(subject);
        // 内容
        message.setText(content);
        // 发信人
        message.setFrom(from);
        javaMailSender.send(message);
    }

    public void htmlEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        // 收信人
        helper.setTo(to);
        // 主题
        helper.setSubject(subject);
        // 内容
        helper.setText(content, true);
        // 发信人
        helper.setFrom(from);
        javaMailSender.send(message);
    }

    public void staticEmail(String to, String subject, String content, Map<String, File> files) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        // 收信人
        helper.setTo(to);
        // 主题
        helper.setSubject(subject);
        // 内容
        helper.setText(content, true);
        // 发信人
        helper.setFrom(from);
        files.forEach((id, file) -> {
            try {
                helper.addInline(id, file);
            } catch (MessagingException ignore) {}
        });
        javaMailSender.send(message);
    }

    public void attachmentEmail(String to, String subject, String content, Map<String, File> files) throws MessagingException {
        System.getProperties().setProperty("mail.mime.splitlongparameters", "false");
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        // 收信人
        helper.setTo(to);
        // 主题
        helper.setSubject(subject);
        // 内容
        helper.setText(content, true);
        // 发信人
        helper.setFrom(from);
        files.forEach((name, file) -> {
            try {
                helper.addAttachment(name, file);
            } catch (MessagingException ignore) {}
        });
        javaMailSender.send(message);
    }

}

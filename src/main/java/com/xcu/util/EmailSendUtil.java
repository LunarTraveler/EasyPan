package com.xcu.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSendUtil {

    private final JavaMailSender mailSender;

    @Value(value = "easypan.admin.emailTitle")
    private String subject;

    @Value(value = "easypan.admin.emailContent")
    private String context1;

    private String context2 = "，验证码的有效期为两分钟！";

    private String from = "LunarTravel@163.com";

    public void sendEmailCode(String to, String code) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setSubject(subject);
        mail.setText(context1 + code + context2);
        mail.setFrom(from);
        mail.setTo(to);

        mailSender.send(mail);
    }

}
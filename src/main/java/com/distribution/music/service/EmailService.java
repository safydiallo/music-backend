package com.distribution.music.service;

import com.distribution.music.dto.EmailContent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mail.from.address:noreply@musique-zig.com}")
    private String fromAddress;

    @Value("${app.mail.from.name:Musique Zig}")
    private String fromName;

    public EmailContent sendVerificationEmail(String to, String token) {
        String link = baseUrl + "/api/auth/verify?token=" + token;
        String subject = "Confirme ton compte";
        String body = "Clique ici pour confirmer ton compte : " + link;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, fromName, "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Impossible d'envoyer l'email de confirmation", e);
        }

        return new EmailContent(fromName, fromAddress, to, subject, body);
    }

    public void sendResetPasswordEmail(String to, String token) {
        String link = baseUrl + "/reset-password?token=" + token;
        String subject = "Réinitialisation de ton mot de passe";
        String body = "Clique ici pour réinitialiser ton mot de passe : " + link;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, fromName, "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Impossible d'envoyer l'email", e);
        }
}
} 
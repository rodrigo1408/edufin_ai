package br.finedu.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Envio real via SMTP (Spring Mail). Requer MAIL_ENABLED=true e as variáveis MAIL_*. */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpPasswordResetMailer implements PasswordResetMailer {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpPasswordResetMailer(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendResetLink(String toEmail, String displayName, String resetUrl) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("EduFin AI - redefinição de senha");
        msg.setText("""
                Olá, %s!

                Recebemos um pedido para redefinir a sua senha no EduFin AI.
                Use o link abaixo em até 30 minutos:

                %s

                Se você não fez este pedido, ignore este e-mail. Nunca compartilhe este link.
                """.formatted(displayName, resetUrl));
        mailSender.send(msg);
    }
}

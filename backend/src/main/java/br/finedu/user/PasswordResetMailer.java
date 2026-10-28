package br.finedu.user;

/**
 * Canal de entrega do link de redefinição de senha. A implementação SMTP só é ativada com
 * {@code app.mail.enabled=true} e um servidor configurado; sem isso a recuperação fica
 * indisponível (nenhum envio é simulado).
 */
public interface PasswordResetMailer {

    void sendResetLink(String toEmail, String displayName, String resetUrl);
}

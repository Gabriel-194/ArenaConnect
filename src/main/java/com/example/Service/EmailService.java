package com.example.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.Setter;
import org.hibernate.pretty.MessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, String> tokenStorage = new ConcurrentHashMap<>();
    private final Map<String, Integer> attemptsStorage = new ConcurrentHashMap<>();
    private Map<String, Integer> resetAttemptsStorage = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    private String gerarToken() {
        int numero = 1000 + secureRandom.nextInt(9000);
        return String.valueOf(numero);
    }

    public void enviarCodigoRecuperacao(String email ){
        try{
            String codigo = gerarToken();
            tokenStorage.put(email, codigo);
            attemptsStorage.put(email, 0);

            MimeMessage mensagem = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(mensagem,"utf-8");

            helper.setTo(email);
            helper.setSubject("Codigo de recuperação - Arena Connect");

            String htmlMsg =
                    "<div style=\"font-family: Arial, sans-serif; background-color: #050505; color: #fff; padding: 40px; text-align: center; border-radius: 12px; border: 1px solid #00ff7f; max-width: 400px; margin: auto; box-shadow: 0 4px 20px rgba(0, 255, 127, 0.2);\">"
                            + "<h2 style=\"color: #00ff7f; margin-bottom: 5px;\">ArenaConnect</h2>"
                            + "<p style=\"color: #ccc; font-size: 14px;\">Recuperação de Senha</p>"
                            + "<p style=\"margin-top: 30px;\">Olá!</p>"
                            + "<p>Recebemos um pedido para redefinir a sua senha. O seu código de verificação é:</p>"
                            + "<h1 style=\"letter-spacing: 8px; color: #00ff7f; background: rgba(0, 255, 127, 0.1); padding: 15px; border-radius: 8px; display: inline-block; border: 1px solid #00ff7f;\">"
                            + codigo +
                            "</h1>"
                            + "<p style=\"color: #777; font-size: 12px; margin-top: 40px;\">Se não solicitou esta alteração, por favor ignore este e-mail.</p>"
                            + "</div>";

            helper.setText(htmlMsg,true);

            mailSender.send(mensagem);
            System.out.println("E-mail HTML enviado com sucesso para: " + email);
        }catch (Exception e) {
            System.err.println("Erro ao enviar e-mail: " + e.getMessage());
            throw new RuntimeException("Falha ao enviar o e-mail de recuperação.");
        }
    }

    public boolean validarToken(String email, String tokenDigitado) {
        int tentativas = attemptsStorage.getOrDefault(email, 0);

        if(tentativas >= 10){
            tokenStorage.remove(email);
            attemptsStorage.remove(email);
            throw new RuntimeException("Conta bloqueada por excesso de tentativas. Gere um novo código.");
        }

        String tokenReal = tokenStorage.get(email);
        if (tokenReal == null) {
            return false;
        }

        if(tokenDigitado.equals(tokenReal)){
            tokenStorage.remove(email);
            attemptsStorage.remove(email);
            return true;

        } else {
            attemptsStorage.put(email,tentativas +1);
            return false;
        }
    }

    public void resetPassword(String email, String token,String newPassword) {
        int tentativas = resetAttemptsStorage.getOrDefault(email, 0);

        if(tentativas >= 5){
            tokenStorage.remove(email);
            resetAttemptsStorage.remove(email);
            throw new RuntimeException("Bloqueado por numero de tentativas");
        }

        String storedToken = tokenStorage.get(email);
        if (storedToken == null || !storedToken.equals(token)) {
            resetAttemptsStorage.put(email, tentativas + 1);
            throw new RuntimeException("token invalido");
        }

        // 3. SUCCESS! The token is correct.
        // TODO: Here is where you call your UserRepository to actually save the new password
        // example: userRepository.updatePassword(email, passwordEncoder.encode(newPassword));

        // 4. Clean up: Remove the token and reset attempts so it can't be used again
        tokenStorage.remove(email);
        resetAttemptsStorage.remove(email);
    }

}

package com.example.Service;

import com.example.Repository.UserRepository;
import io.github.bucket4j.Bucket;
import jakarta.mail.internet.MimeMessage;
import lombok.Setter;
import org.hibernate.pretty.MessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RateLimitService rateLimitService;


    private final Map<String, String> tokenStorage = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    private String gerarToken() {
        int numero = 1000 + secureRandom.nextInt(9000);
        return String.valueOf(numero);
    }

    public void enviarCodigoRecuperacao(String email ){
        try{
            String codigo = gerarToken();
            tokenStorage.put(email, codigo);

            rateLimitService.resetarValidacaoCodigo(email);

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
        if(!rateLimitService.ValidarCodigo(email)){
            tokenStorage.remove(email);
            throw new RuntimeException("Conta bloqueada por excesso de tentativas. Gere um novo código.");
        }

        String tokenReal = tokenStorage.get(email);
        if(tokenReal == null){
            return false;
        }

        if(tokenDigitado.equals(tokenReal)){
            rateLimitService.resetarValidacaoCodigo(email);
            return true;
        }
        return false;
    }

    public void resetPassword(String email, String token,String newPassword) {
        if(!rateLimitService.ResetSenha(email)){
            tokenStorage.remove(email);
            throw new RuntimeException("Bloqueado por numero de tentativas");
        }

        String storedToken = tokenStorage.get(email);
        if(storedToken == null || !storedToken.equals(token)){
            throw new RuntimeException("token invalido");
        }

        userRepository.updatePassword(email, passwordEncoder.encode(newPassword));

        tokenStorage.remove(email);
        rateLimitService.resetarResetSenha(email);
    }

}

package com.example.Controller;

import com.example.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailController {
    @Autowired
    private EmailService emailService;

    @PostMapping("enviar-codigo")
    public ResponseEntity<String> enviarCodigo(@RequestBody Map<String,String> request){
        String email = request.get("email");

        try{
            emailService.enviarCodigoRecuperacao(email);
            return ResponseEntity.ok("codigo enviado com sucesso");
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/validar-codigo")
    public ResponseEntity<?> validarCodigo(@RequestBody Map<String,String> request){
        String email = request.get("email");
        String token = request.get("token");
        try{
            if (emailService.validarToken(email,token)){
                return ResponseEntity.ok("codigo validado");
            }else{
                return ResponseEntity.badRequest().body("codigo invalido");
            }

        } catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

package com.example.Controller;

import com.example.Models.Notificacao;
import com.example.Models.Users;
import com.example.Repository.UserRepository;
import com.example.Service.NotificacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/minhas")
    public ResponseEntity<List<Notificacao>> getMinhasNotificacoes() {
        Long userId = getUsuarioLogadoId();

        List<Notificacao> notificacoes = notificacaoService.buscarMinhasNotificacoes(userId);
        return ResponseEntity.ok(notificacoes);
    }

    @PutMapping("/lidas")
    public ResponseEntity<Void> marcarComoLidas() {
        Long userId = getUsuarioLogadoId();

        notificacaoService.marcarComoLidas(userId);
        return ResponseEntity.ok().build();
    }

    private Long getUsuarioLogadoId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = auth.getName();

        Users user = userRepository.findByEmail(login)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return user.getIdUser().longValue();
    }
}